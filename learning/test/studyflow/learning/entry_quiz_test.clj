(ns studyflow.learning.entry-quiz-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.entry-quiz :as entry-quiz]
            [studyflow.learning.entry-quiz.events :as events]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def course fixture/course-aggregate)
(def course-id (:id course))
(def student-id (new-id))

(defn random-correct-input
  [{:keys [line-input-fields] :as question}]
  {:pre [line-input-fields] :post [(course/answer-correct? question %)]}
  (into {} (map (fn [{:keys [name correct-answers]}]
                  [name (rand-nth (vec correct-answers))])
                (:line-input-fields question))))

(defn random-incorrect-input
  [question]
  {:pre [question (:id question)] :post [(not (course/answer-correct? question %))]}
  (into {} (map (fn [{:keys [name]}]
                  [name "THIS MUST NOT EVER BE A CORRECT input VALUE!@@"])
                (:line-input-fields question))))

(deftest test-commands
  (testing "start quiz"
    (is (command-result= [:ok [(events/started course-id student-id)]]
                         (execute (entry-quiz/start! course-id student-id -1)
                                  [fixture/course-published-event]))))

  (testing "answering"
    (is (command-result= [:ok [(events/question-answered-incorrectly course-id student-id (-> course :entry-quiz :questions first :id) {})]]
                         (execute (entry-quiz/submit-answer! course-id student-id 1 {})
                                  [fixture/course-published-event
                                   (events/started course-id student-id)
                                   (events/instructions-read course-id student-id)])))))
