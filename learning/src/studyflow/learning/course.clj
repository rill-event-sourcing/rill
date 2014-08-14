(ns studyflow.learning.course
  (:require [rill.aggregate :refer [handle-event handle-command]]
            [rill.uuid :refer [new-id]]
            [clojure.string :refer [trim]]
            [studyflow.learning.course.events :as events]
            [studyflow.learning.course.commands :as commands]))

(defrecord Course
    [id chapters])

(defn- find-by-id
  [coll id]
  (first (filter #(= id (:id %)) coll)))

(defn section
  [course section-id]
  {:pre [course section-id]
   :post [%]}
  (find-by-id (mapcat :sections (:chapters course))
              section-id))

(defn questions-for-section
  [course section-id]
  {:pre [course section-id]
   :post [%]}
  (:questions (section course section-id)))

(defn question-for-section
  [course section-id question-id]
  {:pre [course section-id question-id]
   :post [%]}
  (find-by-id (questions-for-section course section-id)
              question-id))

(defn question-for-entry-quiz
  [course question-index]
  (get-in course [:entry-quiz :questions question-index]))

(defn line-input-fields-answers-correct?
  [input-fields input-values]
  (every? (fn [{:keys [name correct-answers]}]
            (when-let [value (get input-values name)]
              (some
               (fn [correct-answer]
                 (= (trim correct-answer)
                    (trim value)))
               correct-answers)))
          input-fields))

(defn multiple-choice-input-fields-answers-correct?
  [input-fields input-values]
  (every? (fn [{:keys [name choices]}]
            (when-let [given-value (get input-values name)]
              (some (fn [{:keys [value correct]}]
                      (and correct
                           (= value given-value)))
                    choices)))
          input-fields))

(defn answer-correct?
  [{:keys [line-input-fields multiple-choice-input-fields] :as question} input-values]
  {:pre [question]}
  (and (line-input-fields-answers-correct? line-input-fields input-values)
       (multiple-choice-input-fields-answers-correct? multiple-choice-input-fields input-values)))

(defmethod handle-event ::events/Published
  [_ event]
  (->Course (:course-id event) (:chapters (:material event))))

(defmethod handle-event ::events/Updated
  [course event]
  (assoc course :chapters (:chapters (:material event))))

(defmethod handle-event ::events/Deleted
  [_ event]
  nil)

(defmethod handle-command ::commands/Publish!
  [course {:keys [course-id material]}]
  (if course
    [:ok [(events/updated course-id material)]]
    [:ok [(events/published course-id material)]]))

(defmethod handle-command ::commands/Delete!
  [course {:keys [course-id]}]
  [:ok [(events/deleted course-id)]])
