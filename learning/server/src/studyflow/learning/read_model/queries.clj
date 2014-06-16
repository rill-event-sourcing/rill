(ns studyflow.learning.read-model.queries
  (:require [studyflow.learning.read-model :as model]))

(defn course-material
  [m course-id]
  (when-let [course (model/get-course m course-id)]
    (model/course-tree course)))

