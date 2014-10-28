(ns studyflow.learning.chapter-quiz.replay
  "Publically replayable events"
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.message :as message]
            [studyflow.learning.chapter-quiz.events :as events :refer [chapter-quiz-id]]))

(defn replay-chapter-quiz
  [store course-id chapter-id student-id]
  (let [aggregate-id (chapter-quiz-id {:student-id student-id :chapter-id chapter-id})
        [head :as events] (retrieve-events store aggregate-id)]
    (when (= ::events/Started (message/type head))
      {:events events
       :aggregate-version (message/number (last events))
       :aggregate-id aggregate-id})))
