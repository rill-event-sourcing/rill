(ns studyflow.learning.web.api.replay-test
  (:require [studyflow.learning.web.api.replay :as replay]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.chapter-quiz.events :as chapter-quiz]
            [studyflow.learning.course.fixture :as fixture]
            [rill.temp-store :refer [given messages=]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [ring.mock.request :refer [request]]
            [studyflow.learning.web.routes :as routes]
            [clout-link.route :refer [uri-for]]))

(def student-id (new-id))
(def course-id #uuid "71a2bd12-00b9-46fd-b52c-019ea4d2e3ea")
(def chapter-id #uuid "8112d048-1189-4b45-a7ba-78da2b0c389d")
(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-total 1)

(def section-test-events [(section-test/created section-id student-id fixture/course-id)
                          (section-test/question-assigned section-id student-id question-id question-total)
                          (section-test/question-answered-correctly section-id student-id question-id {"__INPUT_1__" "Incorrect input"})])

(def chapter-quiz-events [(chapter-quiz/started course-id chapter-id student-id false)
                          (chapter-quiz/question-assigned course-id chapter-id student-id (new-id) (new-id))
                          (chapter-quiz/question-answered-incorrectly course-id chapter-id student-id (new-id) {"_INPUT_1_" "2"
                                                                                                                "_INPUT_2_" "2"})])

(deftest test-replay-api
  (let [store (given (cons fixture/course-published-event (concat section-test-events chapter-quiz-events)))]
    (is (messages= section-test-events
                   (-> (replay/handler (-> (request :get (uri-for routes/section-test-replay section-id student-id))
                                           (assoc-in [:student :student-id] student-id)
                                           (assoc :event-store store)))
                       :body :events)))
    (is (messages= chapter-quiz-events
                   (-> (replay/handler (-> (request :get (uri-for routes/chapter-quiz-replay course-id chapter-id student-id))
                                           (assoc-in [:student :student-id] student-id)
                                           (assoc :event-store store)))
                       :body :events)))))
