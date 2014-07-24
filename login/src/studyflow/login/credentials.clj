(ns studyflow.login.credentials
  (:require [clojure.core.async :refer [<!! thread]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.events.student :as student-events]))

(defn authenticate [db email password]
  (if-let [user (get db email)]
    (if (bcrypt/check password (:encrypted-password user))
      user)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event sourcing

(defmulti handle-event (fn [_ event] (message/type event)))

(defmethod handle-event ::student-events/CredentialsAdded
  [state {:keys [email student-id encrypted-password]}]
  (assoc state email
         {:user-id student-id
          :user-role "student"
          :encrypted-password encrypted-password}))

(defmethod handle-event ::student-events/CredentialsChanged
  [state {:keys [email student-id encrypted-password]}]
  (into {email
          {:user-id student-id
           :user-role "student"
           :encrypted-password encrypted-password }}
        (filter (fn [[_ user]] (not= student-id (:user-id user))) state)))

(defmethod handle-event :default
  [state _] state)

(defn listen! [channel store]
  (thread
    (loop []
      (when-let [event (<!! channel)]
        (swap! store handle-event event)
        (recur)))))
