(ns studyflow.learning.write-model
  (:require [rill.aggregate :refer [handle-event defaggregate]]
            [studyflow.events :as events]
            [rill.message :as message]))

(defaggregate Course [])

(defmethod handle-event [nil :course-published]
  [_ event]
  (->Course (:course-id event)))

(defmethod handle-event [Course :course-updated]
  [course event]
  course)
