(ns studyflow.login.edu-route-student
  (:require [studyflow.login.edu-route-student.events :as events]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [schema.core :as s]))

(defrecord EduRouteStudent
    [id])

(defcommand Register!
  :edu-route-id s/Str
  :full-name s/Str
  :brin-code s/Str
  events/edu-route-student-stream-id)

(defmethod handle-command ::Register!
  [edu-route-student {:keys [edu-route-id full-name brin-code]}]
  {:pre [edu-route-id full-name brin-code]}
  (if (not edu-route-student)
    [:ok [(events/registered edu-route-id full-name brin-code)]]
    [:rejected]))

(defmethod handle-event ::events/Registered
  [edu-route-student {:keys [edu-route-id full-name brin-code]}]
  {})

