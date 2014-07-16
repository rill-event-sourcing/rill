(ns studyflow.login.credentials
  (:require [clojure.core.async :refer [<!! thread]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.events.student :as student-events]
            [clojure.tools.logging :as log]))

(defn authenticate-by-email-and-password [db email password]
  (if-let [user (get-in db [:by-email email])]
    (if (bcrypt/check password (:encrypted-password user))
      user)))

(defn authenticate-by-edu-route-id [db edu-route-id]
  (let [user (get-in db [:by-edu-route-id edu-route-id])]
    (log/info [:authenticated-as user])
    user))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event sourcing

(defmulti handle-event (fn [_ event] (message/type event)))

(defmethod handle-event ::student-events/CredentialsAdded
  [db {:keys [email student-id encrypted-password]}]
  (assoc-in db [:by-email email]
            {:uuid student-id
             :role "student"
             :encrypted-password encrypted-password}))

(defmethod handle-event ::student-events/CredentialsChanged
  [db {:keys [email student-id encrypted-password]}]
  (assoc db :by-email
         (into {email
                {:uuid student-id
                 :role "student"
                 :encrypted-password encrypted-password }}
               (filter (fn [[_ user]] (not= student-id (:uuid user))) db))))

(defmethod handle-event ::student-events/EduRouteCredentialsAdded
  [db {:keys [edo-route-id student-id]}]
  (assoc-in db [:by-edu-route-id edo-route-id]
            {:uuid student-id
             :role "student"}))

(defmethod handle-event :default
  [db _] db)

(defn listen! [channel db]
  (thread
    (loop []
      (when-let [event (<!! channel)]
        (swap! db handle-event event)
        (recur)))))
