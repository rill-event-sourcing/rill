(ns studyflow.learning.entry-quiz
  (:require [rill.aggregate :refer [handle-event handle-command]]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.entry-quiz.events :as events]
            [studyflow.learning.entry-quiz.commands :as commands]))

(defrecord EntryQuiz
    [id questions])

(defmethod handle-event ::events/Published
  [_ event]
  (->EntryQuiz (:entry-quiz-id event) (:questions (:material event))))

(defmethod handle-event ::events/Updated
  [course event]
  (assoc course :questions (:questions (:material event))))

(defmethod handle-event ::events/Deleted
  [_ event]
  nil)

(defmethod handle-command ::commands/Publish!
  [entry-quiz {:keys [entry-quiz-id material]}]
  (if entry-quiz
    [:ok [(events/updated entry-quiz-id material)]]
    [:ok [(events/published entry-quiz-id material)]]))
