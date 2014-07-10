(ns studyflow.school-administration.student
  (:require [studyflow.school-administration.student.events :as events]
            [studyflow.school-administration.student.commands :as commands]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]))

(defaggregate Student [student-id])

(defmethod handle-command ::commands/Create!
  [student {:keys [student-id full-name]}]
  {:pre [(nil? student)]}
  [(events/created student-id full-name)])
