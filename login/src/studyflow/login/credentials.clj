(ns studyflow.login.credentials
  (:require [clojure.core.async :refer [<!! thread]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.events.student :as student-events]))

(defonce db-atom (atom {"editor@studyflow.nl" {:uuid "editor-id" :role "editor" :encrypted-password (bcrypt/encrypt "editor")}}))

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
         {:uuid student-id
          :role "student"
          :encrypted-password encrypted-password}))

(defmethod handle-event ::student-events/CredentialsChanged
  [state {:keys [email student-id encrypted-password]}]
  (into {email
         {:uuid student-id
          :role "student"
          :encrypted-password encrypted-password }}
        (filter (fn [[_ user]] (not= student-id (:uuid user))) state)))

(defmethod handle-event :default
  [state _] state)

(defn listen! [channel store]
  (thread
    (loop []
      (when-let [event (<!! channel)]
        (swap! store handle-event event)
        (recur)))))
