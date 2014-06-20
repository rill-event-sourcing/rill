(ns studyflow.learning.read-model.event-handler
  (:require [studyflow.learning.read-model :as m]
            [studyflow.events :as events])
  (:import (studyflow.events CoursePublished CourseUpdated CourseDeleted)))

(defmulti handle-event
  "Update the read model with the given event"
  (fn [model event] (class event)))

(defn update-model
  [model events]
  (reduce handle-event model events))

(defn init-model
  [initial-events]
  (update-model nil initial-events)) 

(defmethod handle-event CoursePublished
  [model event]
  (m/set-course model (:course-id event) (:material event)))

(defmethod handle-event CourseUpdated
  [model event]
  (m/set-course model (:course-id event) (:material event)))

(defmethod handle-event CourseDeleted
  [model event]
  (m/remove-course model (:course-id event)))





