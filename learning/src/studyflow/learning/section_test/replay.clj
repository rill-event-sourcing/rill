(ns studyflow.learning.section-test.replay
  "Publically replayable events"
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.message :as message]
            [studyflow.learning.section-test.events :as events]))

(defn replay-section-test
  [store section-test-id]
  (let [[head :as events] (retrieve-events store section-test-id)]
    (when (= ::events/Created (message/type head))
      ;; TODO: Check for student id here...
      {:events events
       :aggregate-version (:number (last events))
       :aggregate-id section-test-id})))
