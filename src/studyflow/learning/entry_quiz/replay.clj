(ns studyflow.learning.entry-quiz.replay
  "Publically replayable events"
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.message :as message]
            [studyflow.learning.entry-quiz.events :as events :refer [entry-quiz-id]]))

(defn replay-entry-quiz
  [store course-id student-id]
  (let [aggregate-id (entry-quiz-id {:student-id student-id :course-id course-id})
        [head :as events] (retrieve-events store aggregate-id)]
    (when (#{::events/Started
             ::events/NagScreenDismissed} (message/type head))
      {:events events
       :aggregate-version (message/number (last events))
       :aggregate-id aggregate-id})))
