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
    (log/info [:authenticated-as user])
    user))

;;;; Accessors for credentials db

(defn add-email-and-password-credentials
  [db student-id {:keys [email encrypted-password]}]
  (assoc-in db [:by-email email]
            {:user-id student-id
             :user-role "student"
             :encrypted-password encrypted-password}))

(defn change-email-and-password-credentials
  [db student-id {:keys [email encrypted-password]}]
  (assoc db :by-email
         (into {email
                {:user-id student-id
                 :user-role "student"
                 :encrypted-password encrypted-password }}
               (filter (fn [[_ user]] (not= student-id (:user-id user))) db))))

(defn change-email
  [db student-id {:keys [email]}]
  (let [[old-email cred]
        (first (filter (fn [[_ {:keys [user-id]}]] (= student-id user-id))
                       (:by-email db)))]
    (-> db
        (update-in [:by-email] dissoc old-email)
        (assoc-in [:by-email email] cred))))

(defn add-edu-route-credentials
  [db student-id edu-route-id]
  (assoc-in db [:by-edu-route-id edu-route-id]
            {:user-id student-id
             :user-role "student"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event sourcing

(defmulti handle-event (fn [_ event] (message/type event)))

(defmethod handle-event :studyflow.school-administration.student.events/CredentialsAdded
  [db {:keys [student-id credentials]}]
  (add-email-and-password-credentials db student-id credentials))

(defmethod handle-event :studyflow.school-administration.student.events/CredentialsChanged
  [db {:keys [student-id credentials]}]
  (change-email-and-password-credentials db student-id credentials))

(defmethod handle-event :studyflow.school-administration.student.events/EmailChanged
  [db {:keys [student-id email]}]
  (change-email db student-id email))

(defmethod handle-event :studyflow.school-administration.student.events/EduRouteCredentialsAdded
  [db {:keys [edu-route-id student-id] :as event}]
  (add-edu-route-credentials db student-id edu-route-id))

(defmethod handle-event :default
  [db _]
  (log/info :skipped-event)
  db)

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
