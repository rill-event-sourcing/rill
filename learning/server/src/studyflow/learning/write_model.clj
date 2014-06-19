(ns studyflow.learning.write-model
  (:require [rill.aggregate :refer [handle-event defaggregate]])
  (:import (studyflow.events CourseUpdated CoursePublished)))

(defaggregate Course [])

(defmethod handle-event [nil CoursePublished]
  [_ event]
  (->Course (:course-id event)))

(defmethod handle-event [Course CourseUpdated]
  [course event]
  course)


