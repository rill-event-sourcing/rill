(ns studyflow.login.credentials
  (:require [clojure.core.async :refer [<!! thread]]
            [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [rill.event-channel :as event-channel]
            [clojure.tools.logging :as log]))



;;;; Authenticate against the credentials db

(defn authenticate-by-email-and-password [db email password]
  (if-let [user (get-in db [:by-email email])]
    (if (bcrypt/check password (:encrypted-password user))
      user)))

(defn authenticate-by-edu-route-id [db edu-route-id]
  (let [user (get-in db [:by-edu-route-id edu-route-id])]
    (log/debug [:authenticated-as user])
    user))

;;;; Accessors for credentials db

(defn add-email-and-password-credentials
  [db user-id {:keys [email encrypted-password]} role]
  (-> db
      (assoc-in [:by-email email]
                {:user-id user-id
                 :user-role role
                 :encrypted-password encrypted-password})
      (assoc-in [:email-by-id user-id] email)))

(defn change-email-and-password-credentials
  [db user-id {:keys [email encrypted-password]} role]
  (let [old-email (get-in db [:email-by-id user-id])]
    (-> db
        (update-in [:by-email] dissoc old-email)
        (assoc-in [:by-email email] {:user-id user-id
                                     :user-role role
                                     :encrypted-password encrypted-password})
        (assoc-in [:email-by-id user-id] email))))

(defn change-email
  [db user-id {:keys [email]}]
  (let [old-email (get-in db [:email-by-id user-id])
        cred (get-in db [:by-email old-email])]
    (if cred
      (-> db
          (update-in [:by-email] dissoc old-email)
          (assoc-in [:by-email email] cred)
          (assoc-in [:email-by-id user-id] email))
      db)))

(defn add-edu-route-credentials
  [db user-id edu-route-id]
  (assoc-in db [:by-edu-route-id edu-route-id]
            {:user-id user-id
             :user-role "student"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event sourcing

(defmulti handle-event (fn [_ event] (message/type event)))

(defmethod handle-event :studyflow.school-administration.student.events/CredentialsAdded
  [db {:keys [student-id credentials]}]
  (add-email-and-password-credentials db student-id credentials "student"))

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [db {:keys [student-id credentials]}]
  (add-email-and-password-credentials db student-id credentials "student"))

(defmethod handle-event :studyflow.school-administration.student.events/CredentialsChanged
  [db {:keys [student-id credentials]}]
  (change-email-and-password-credentials db student-id credentials "student"))

(defmethod handle-event :studyflow.school-administration.student.events/EmailChanged
  [db {:keys [student-id email]}]
  (change-email db student-id email))

(defmethod handle-event :studyflow.school-administration.student.events/EduRouteCredentialsAdded
  [db {:keys [edu-route-id student-id] :as event}]
  (add-edu-route-credentials db student-id edu-route-id))

(defmethod handle-event :studyflow.school-administration.teacher.events/CredentialsAdded
  [db {:keys [teacher-id credentials]}]
  (add-email-and-password-credentials db teacher-id credentials "teacher"))

(defmethod handle-event :studyflow.school-administration.teacher.events/CredentialsChanged
  [db {:keys [teacher-id credentials]}]
  (change-email-and-password-credentials db teacher-id credentials "teacher"))

(defmethod handle-event :studyflow.school-administration.teacher.events/EmailChanged
  [db {:keys [teacher-id email]}]
  (change-email db teacher-id email))

(defmethod handle-event :default [db _] db)

;; catchup

(defn caught-up
  [db]
  (assoc db :caught-up true))

(defn caught-up?
  [db]
  (boolean (:caught-up db)))

(defmethod handle-event ::event-channel/CaughtUp
  [db _]
  (caught-up db))

;; listener

(defn listen! [channel db]
  (thread
    (loop []
      (when-let [event (<!! channel)]
        (swap! db handle-event event)
        (recur)))))
