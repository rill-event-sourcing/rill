(ns studyflow.learning.student-entry-quiz
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.student-entry-quiz.events :as events]
            [studyflow.learning.student-entry-quiz.commands :as commands]
            [studyflow.learning.entry-quiz-material :as entry-quiz-material]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord StudentEntryQuiz
    [entry-quiz-id
     student-id
     status])

(defmethod aggregate-ids ::commands/Init!
  [{:keys [entry-quiz-id]}]
  [entry-quiz-id])

(defmethod handle-command ::commands/Init!
  [entry-quiz {:keys [entry-quiz-id student-id] :as command} entry-quiz-material]
  {:pre [(nil? entry-quiz)
         entry-quiz-id
         student-id
         entry-quiz-material]}
  [:ok [(events/created entry-quiz-id student-id)]])

(defmethod aggregate-ids ::commands/Dismiss!
  [{:keys [entry-quiz-id]}]
  [entry-quiz-id])

(defmethod handle-command ::commands/Dismiss!
  [entry-quiz command entry-quiz-material]
  {:pre []}
  [:ok []])

(defmethod handle-event ::events/Created
  [_ event]
  nil #_ (->EntryQuiz :etc)
)
