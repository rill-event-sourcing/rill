(ns studyflow.learning.read-model
  (:require [studyflow.events :as events])
  (:import (studyflow.events ChapterPublished ChapterUpdated ChapterArchived
                             LearningStepUpdated LearningStepPublished LearningStepArchived
                             CoursePublished CourseUpdated CourseArchived)))


(defmulti handle-event
  "Update the read model with the given event"
  (fn [model event] (class event)))

(defn set-chapter
  [model props]
  (assoc-in model [:chapters (:chapter-id props)]
            (select-keys props [:title :description :learning-step-ids])))

(defn remove-chapter
  [model props]
  (update-in model [:chapters] #(dissoc % (:chapter-id props))))

(defn get-chapter [model id]
  (get-in model [:chapters id]))

(defmethod handle-event ChapterPublished [model event]
  (set-chapter model event))

(defmethod handle-event ChapterUpdated [model event]
  (set-chapter model event))

(defmethod handle-event ChapterArchived [model event]
  (remove-chapter model event))

(defn set-learning-step
  [model props]
  (assoc-in model [:learning-steps (:learning-step-id props)]
            (select-keys props [:title :task-ids])))

(defn remove-learning-step
  [model props]
  (update-in model [:learning-steps] #(dissoc % (:learning-step-id props))))

(defn get-learning-step
  [model id]
  (get-in model [:learning-steps id]))

(defmethod handle-event LearningStepPublished [model props]
  (set-learning-step model props))

(defmethod handle-event LearningStepUpdated [model props]
  (set-learning-step model props))

(defmethod handle-event LearningStepArchived [model props]
  (remove-learning-step model props))

(defn update-model
  [model events]
  (reduce handle-event model events))

(defn set-course
  [model props]
  (assoc-in model [:courses (:course-id props)] (select-keys props [:title :chapter-ids])))

(defn remove-course
  [model props]
  (update-in model [:courses] #(dissoc % (:course-id props))))

(defmethod handle-event CoursePublished
  [model event]
  (set-course model event))

(defmethod handle-event CourseUpdated
  [model event]
  (set-course model event))

(defmethod handle-event CourseArchived
  [model event]
  (remove-course model event))

(defn chapter-tree
  [model id]
  (let [chapter (get-chapter model id)]
    {:id id
     :title (:title chapter)
     :learning-steps (mapv (fn [id]
                             {:id id
                              :title (:title (get-learning-step model id))})
                           (:learning-step-ids chapter))}))
(defn get-course
  [model id]
  (get-in model [:courses id]))

(defn course-tree
  [model id]
  (let [course (get-course model id)] {:title (:title course)
           :id id
           :chapters (mapv (partial chapter-tree model) (:chapter-ids course))}))

(defn get-course-tree
  [model id]
  (select-keys (get-in model [:courses id]) [:title]))
