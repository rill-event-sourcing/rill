(ns studyflow.learning.section-test-test
  (:require [studyflow.learning.section-test :as section-test]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def section-test-id (new-id))
(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")

(def course fixture/course-aggregate)

(defn random-correct-input
  [{:keys [input-fields] :as question}]
  {:pre [input-fields] :post [(course/answer-correct? question %)]}
  (into {} (map (fn [{:keys [name correct-answers]}]
                  [name (rand-nth (vec correct-answers))])
                (:input-fields question))))

(defn random-incorrect-input
  [question]
  {:pre [question (:id question)] :post [(not (course/answer-correct? question %))]}
  (into {} (map (fn [{:keys [name]}]
                  [name "THIS MUST NOT EVER BE A CORRECT input VALUE!@@"])
                (:input-fields question))))

(defn apply-command
  "handle command and use the generated events to update the aggregate. Returns the new aggregate state"
  [aggregate command & aggregates]
  (update-aggregate aggregate (apply handle-command aggregate command aggregates)))


(deftest test-section-test-flow
  (let [section-test (apply-command nil (commands/init! section-test-id section-id (:id course)) course)
        question-id (:current-question-id section-test)]
    (testing "initialization"
      (is (= (:id section-test) section-test-id))
      (is (contains? (set (map :id (course/questions-for-section course section-id))) (:current-question-id section-test))))

    (let [question (course/question-for-section course section-id (:current-question-id section-test))
          correct (apply-command section-test
                                 (commands/check-answer! section-test-id section-id (:id course) (:id question)
                                                         (random-correct-input question))
                                 course)
          incorrect (apply-command section-test
                                   (commands/check-answer! section-test-id section-id (:id course) (:id question)
                                                           (random-incorrect-input question))
                                   course)]

      (testing "correctly answer a question without errors"
        (is (= (:current-question-status correct)
               :answered-correctly)))

      (testing "incorrectly answer a question"
        (is (= (:current-question-status incorrect)
               :answered-incorrectly))))))
