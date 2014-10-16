(ns studyflow.learning.section-test.replay-test
  (:require [studyflow.learning.section-test.replay :as replay]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.course.fixture :as fixture]
            [rill.temp-store :refer [given messages=]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]))

(def student-id (new-id))
(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-total 1)

(def section-test-events [(events/created section-id student-id fixture/course-id)
                          (events/question-assigned section-id student-id question-id question-total)
                          (events/question-answered-correctly section-id student-id question-id {"_INPUT_1_" "Incorrect input"})])

(deftest test-replay-api
  (let [store (given (cons fixture/course-published-event section-test-events))]
    (is (messages= section-test-events
                   (:events (replay/replay-section-test store section-id student-id))))) )
