(ns studyflow.login.edu-route-student.events
  (:require [rill.message :refer [defevent primary-aggregate-id]]
            [schema.core :as s]))

(defn edu-route-student-stream-id
  [msg]
  (str "edu-route-student:" (:edu-route-id msg)))

(defevent Registered
  :edu-route-id s/Str
  :full-name s/Str
  :brin-code s/Str
  edu-route-student-stream-id)



