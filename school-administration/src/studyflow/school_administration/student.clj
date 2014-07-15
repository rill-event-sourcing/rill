(ns studyflow.school-administration.student
  (:require [studyflow.school-administration.student.events :as events]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]
            [rill.message :refer [defcommand]]
            [schema.core :as s]))

(defaggregate Student [student-id])

(defcommand Create!
  :student-id s/Uuid
  :full-name s/Str)

(defmethod handle-command ::Create!
  [student {:keys [student-id full-name]}]
  {:pre [(nil? student)]}
  [(events/created student-id full-name)])
