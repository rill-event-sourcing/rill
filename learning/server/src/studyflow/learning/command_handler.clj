(ns studyflow.learning.command-handler
  (:require [studyflow.events :as evt]
            [studyflow.learning.commands]
            [studyflow.learning.write-model]
            [rill.handler :refer [handle-command defaggregate-ids]]
            [rill.uuid :refer [new-id]])
  (:import (studyflow.learning.commands PublishCourse! UpdateCourse! DeleteCourse!)))

(defaggregate-ids PublishCourse! course-id)
(defaggregate-ids UpdateCourse! course-id)
(defaggregate-ids DeleteCourse! course-id)

(defmethod handle-command PublishCourse!
  [command course]
  (if course
    [(evt/strict-map->CourseUpdated (-> command
                                          (select-keys [:course-id :material])
                                          (assoc :id (new-id))))]
    [(evt/strict-map->CoursePublished (-> command
                                        (select-keys [:course-id :material])
                                        (assoc :id (new-id))))]))

(defmethod handle-command DeleteCourse!
  [command course]
  (when course
    [(evt/strict-map->CourseDeleted (-> command
                                        (select-keys [:course-id])
                                        (assoc :id (new-id))))]))

;; (defmethod aggregates StartLearningStep!
;;   [command]
;;   (map command [:learning-step-id :work-id :student-id]))

;; (defmethod handle-command OpenLearningStep!
;;   [command learning-step work student]
;;   (cond (nil? work)
;;         (evt/map->StudentStartedWorkOnLearningStep (select-keys command [:student-id :work-id :learning-step-id]))

;;         (= (select-keys work [:student-id :work-id :learning-step-id])
;;            (select-keys command [:student-id :work-id :learning-step-id]))
;;         (evt/map->StudentContinuedWorkOnLearningStep (select-keys command [:student-id :work-id :learning-step-id]))))


;; (defmethod aggregates AnswerQuestion!
;;   [command]
;;   (map command [:work-id :student-id :task-id :question-id]))

;; (defmethod handle-command AnswerQuestion!
;;   [{value :value} student work task question]
;;   (get (:revealed-question-ids work)))
