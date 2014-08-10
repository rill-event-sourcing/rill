(ns studyflow.learning.student-entry-quiz.replay
  "Publically replayable events"
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.message :as message]
            [studyflow.learning.student-entry-quiz.events :as events :refer [student-entry-quiz-id]]))

(defn replay-student-entry-quiz
  [store entry-quiz-id student-id]
  (let [aggregate-id (student-entry-quiz-id {:student-id student-id :entry-quiz-id entry-quiz-id})
        [head :as events] (retrieve-events store aggregate-id)]
    (when (= ::events/Created (message/type head))
      {:events events
       :aggregate-version (message/number (last events))
       :aggregate-id aggregate-id})))
