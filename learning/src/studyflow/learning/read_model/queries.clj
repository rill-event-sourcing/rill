(ns studyflow.learning.read-model.queries
  (:require [studyflow.learning.read-model :as model]))

(defn course-material
  [m course-id student-id]
  (model/course-tree m course-id student-id))

(defn section
  [m course-id section-id]
  (-> m
      (model/get-course course-id)
      (model/get-section section-id)
      (dissoc :questions)))

(defn remove-answers [question]
  (-> question
      (update-in [:multiple-choice-input-fields]
                 (fn [inputs]
                   (->> (for [input inputs]
                          (update-in input [:choices]
                                     (fn [choices]
                                       (mapv #(dissoc % :correct) choices))))
                        (into []))))
      (update-in [:line-input-fields]
                 (fn [inputs]
                   (->> (for [input inputs]
                          (dissoc input :correct-answers))
                        (into []))))
      (assoc :has-worked-out-answer (contains? question :worked-out-answer))
      (dissoc :worked-out-answer)))

(defn question
  [m course-id section-id question-id]
  (-> m
      (model/get-course course-id)
      (model/get-section section-id)
      (model/get-question question-id)
      remove-answers))


(defn entry-quiz
  [m course-id]
  (-> m
      (model/get-course course-id)
      :entry-quiz
      (update-in [:questions] (partial map remove-answers))))
