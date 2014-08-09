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
  [entry-quiz command entry-quiz-material]
  {:pre []}
  [:ok []
   ])

(defmethod handle-event ::events/Created
  [_ event]
  nil #_ (->EntryQuiz :etc)
)
