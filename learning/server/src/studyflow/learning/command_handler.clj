(ns studyflow.learning.command-handler
  (:require [studyflow.events :as evt]
            [rill.message :as m]
            [studyflow.learning.commands :as commands]
            [studyflow.learning.write-model]
            [rill.handler :refer [handle-command defaggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defaggregate-ids :publish-course! course-id)
(defaggregate-ids :update-course! course-id)
(defaggregate-ids :delete-course! course-id)

(defmethod handle-command :publish-course!
  [command course]
  (if course
    [(evt/map->CourseUpdated (-> command
                                 (select-keys [:course-id :material])
                                 (assoc :m/id (new-id))))]
    [(evt/map->CoursePublished (-> command
                                   (select-keys [:course-id :material])
                                   (assoc :m/id (new-id))))]))

(defmethod handle-command :delete-course!
  [command course]
  (when course
    [(evt/map->CourseDeleted (-> command
                                 (select-keys [:course-id])
                                 (assoc :m/id (new-id))))]))

