(ns studyflow.learning.read-model.queries
  (:require [studyflow.learning.read-model :as model]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-local-date]]))

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

(defn remove-answers-from-chapter-quiz-question [question]
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
                        (into []))))))

(defn course-material
  [m course-id student-id]
  (-> (model/course-tree m course-id student-id)
      (update-in [:entry-quiz :questions] #(map remove-answers %))
      (assoc :total-coins (model/total-coins m course-id student-id))))

(defn section
  [m course-id section-id]
  (-> m
      (model/get-course course-id)
      (model/get-section section-id)
      (dissoc :questions)))

(defn question
  [m course-id section-id question-id]
  (some-> m
          (model/get-course course-id)
          (model/get-section section-id)
          (model/get-question question-id)
          remove-answers))

(defn chapter-quiz-question
  [m course-id chapter-id question-id]
  (some-> m
          (model/get-course course-id)
          (model/get-chapter chapter-id)
          (model/get-chapter-quiz-question question-id)
          remove-answers-from-chapter-quiz-question))

(defn leaderboard
  [m course-id student-id]
  (-> (model/leaderboard m course-id (to-local-date (now))
                         (:school-id (model/school-for-student m student-id)))
      (model/personalized-leaderboard student-id)))
