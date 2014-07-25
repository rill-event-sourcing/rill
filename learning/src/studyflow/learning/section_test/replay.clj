(ns studyflow.learning.section-test.replay
  "Publically replayable events"
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.message :as message]
            [studyflow.learning.section-test.events :as events :refer [section-test-id]]))

(defn replay-section-test
  [store section-id student-id]
  (let [aggregate-id (section-test-id {:student-id student-id :section-id section-id})
        [head :as events] (retrieve-events store aggregate-id)]
    (when (= ::events/Created (message/type head))
      {:events events
       :aggregate-version (message/number (last events))
       :aggregate-id aggregate-id})))
