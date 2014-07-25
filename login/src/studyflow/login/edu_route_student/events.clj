(ns studyflow.login.edu-route-student.events
  (:require [rill.message :refer [defevent primary-aggregate-id]]
            [schema.core :as s]))

(defevent Registered
  :edu-route-id s/Str
  :full-name s/Str
  :brin-code s/Str)

(defn edu-route-student-stream-id
  [msg]
  (str "edu-route-student:" (:edu-route-id msg)))

(defmethod primary-aggregate-id ::Registered
  [event]
  (edu-route-student-stream-id event))


