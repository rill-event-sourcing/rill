(ns studyflow.learning.read-model.queries
  (:require [studyflow.learning.read-model :as model]))

(defn course-material
  [m course-id]
  (when-let [course (model/get-course m course-id)]
    (model/course-tree course)))

(defn section
  [m course-id section-id]
  (model/get-section (model/get-course m course-id) section-id))

(defn remove-answers [question]
  (-> question
      (update-in [:multiple-choice-input-fields]
                 (fn [inputs]
                   (->> (for [input inputs]
                          (update-in input [:choices]
                                     (fn [choices]
                                       (into #{} (map #(dissoc % :correct) choices)))))
                        (into #{}))))
      (update-in [:line-input-fields]
                 (fn [inputs]
                   (->> (for [input inputs]
                          (dissoc input :correct-answers))
                        (into #{}))))
      (dissoc :worked-out-answer)))

(defn question
  [m course-id section-id question-id]
  (-> m
      (model/get-course course-id)
      (model/get-section section-id)
      (model/get-question question-id)
      remove-answers))

