(ns studyflow.learning.read-model.queries
  (:require [studyflow.learning.read-model :as model]))

(defn course-material
  [m course-id]
  (when-let [course (model/get-course m course-id)]
    (model/course-tree course)))

(defn section
  [m course-id section-id]
  (model/get-section (model/get-course m course-id) section-id))

(defn question
  [m course-id section-id question-id]
  (-> m
      (model/get-course course-id)
      (model/get-section section-id)
      (model/get-question question-id)))

