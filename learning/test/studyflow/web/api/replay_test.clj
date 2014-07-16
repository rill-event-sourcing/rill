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

(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def section-test-id (str "student-idSTUDENTMOCKsection-id" section-id))
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")

(def section-test-events [(events/created section-test-id fixture/course-id section-id)
                          (events/question-assigned section-test-id fixture/course-id question-id)
                          (events/question-answered-correctly section-test-id question-id {"__INPUT_1__" "Incorrect input"})])

(deftest test-replay-api
  (let [store (given (cons fixture/course-published-event section-test-events))]
    (is (messages= section-test-events
                   (-> (replay/handler (-> (request :get (uri-for routes/section-test-replay section-test-id))
                                           (assoc :event-store store)))
                       :body :events)))))
