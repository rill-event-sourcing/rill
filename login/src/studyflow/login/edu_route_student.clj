(ns studyflow.login.edu-route-student
  (:require [studyflow.login.edu-route-student.events :as events]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]
            [schema.core :as s]))

(defaggregate EduRouteStudent [])

(defcommand Register!
  :edu-route-id s/Str
  :full-name s/Str
  :brin-code s/Str)

(defmethod primary-aggregate-id ::Register!
  [command]
  (events/edu-route-student-stream-id command))

(defmethod handle-command ::Register!
  [edu-route-student {:keys [edu-route-id full-name brin-code]}]
  {:pre [edu-route-id full-name brin-code]}
  (when (not edu-route-student)
    [(events/registered edu-route-id full-name brin-code)]))

(defmethod handle-event ::events/Registered
  [edu-route-student {:keys [edu-route-id full-name brin-code]}]
  {})

