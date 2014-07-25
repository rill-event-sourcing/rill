(ns studyflow.web.api.replay-test
  (:require [studyflow.web.api.replay :as replay]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.course.fixture :as fixture]
            [rill.temp-store :refer [given messages=]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [ring.mock.request :refer [request]]
            [studyflow.web.routes :as routes]
            [clout-link.route :refer [uri-for]]))

(def student-id (new-id))
(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")

(def section-test-events [(events/created section-id student-id fixture/course-id)
                          (events/question-assigned section-id student-id question-id)
                          (events/question-answered-correctly section-id student-id question-id {"__INPUT_1__" "Incorrect input"})])

(deftest test-replay-api
  (let [store (given (cons fixture/course-published-event section-test-events))]
    (is (messages= section-test-events
                   (-> (replay/handler (-> (request :get (uri-for routes/section-test-replay section-id student-id))
                                           (assoc :event-store store)))
                       :body :events)))))
