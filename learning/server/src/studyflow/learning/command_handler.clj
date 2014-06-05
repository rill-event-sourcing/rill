(ns studyflow.learning.command-handler
  (:require [studyflow.events :as evt]
            [studyflow.learning.commands]
            [rill.handler :refer [handle-command defaggregate-ids]])
  (:import (studyflow.learning.commands PublishCourse! UpdateCourse! DeleteCourse!)))


(defaggregate-ids PublishCourse! course-id)
(defaggregate-ids UpdateCourse! course-id)
(defaggregate-ids DeleteCourse! course-id)

(defmethod handle-command PublishCourse!
  [command course]
  (when-not course
    [(evt/strict-map->CoursePublished (select-keys command [:course-id :publisher-id :material]))]))

(defmethod handle-command UpdateCourse!
  [command course]
  (when course
    [(evt/strict-map->CourseUpdated (select-keys command [:course-id :publisher-id :material]))]))

(defmethod handle-command DeleteCourse!
  [command course]
  (when course
    [(evt/strict-map->CourseDeleted (select-keys command [:course-id :publisher-id]))]))

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


