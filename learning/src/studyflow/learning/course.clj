(ns studyflow.learning.course
  (:require [rill.aggregate :refer [defaggregate handle-event handle-command]]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.course.events :as events]
            [studyflow.learning.course.commands :as commands]))

(defaggregate Course [chapters])

(defn- find-by-id
  [coll id]
  (first (filter #(= id (:id %)) coll)))

(defn section
  [course section-id]
  (find-by-id (mapcat :sections (:chapters course))
              section-id))

(defn questions-for-section
  [course section-id]
  (:questions (section course section-id)))

(defn question-for-section
  [course section-id question-id]
  (find-by-id (questions-for-section course section-id)
              question-id))

(defn answer-correct?
  [{:keys [line-input-fields]} input-values]
  (every? (fn [{:keys [name correct-answers]}]
            (when-let [value (get input-values name)]
              (contains? correct-answers value)))
          line-input-fields))

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
    [(events/updated course-id material)]
    [(events/published course-id material)]))

(defmethod handle-command ::commands/Delete!
  [course {:keys [course-id]}]
  [(events/deleted course-id)])
