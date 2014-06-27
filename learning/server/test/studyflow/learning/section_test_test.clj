(ns studyflow.learning.section-test-test
  (:require [studyflow.learning.section-test :as section-test]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [with-temp-store]]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def section-test-id (new-id))
(def section-id #uuid "8117bf7b-8025-43ea-b6d3-aa636d6b6042")
(def question-id #uuid "b117bf7b-8025-43ea-b6d3-aa636d6b6042")

(def course fixture/course-aggregate)
(def course-id (:id course))

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

(deftest test-section-test-flow
  (testing "correct flow"
    (with-temp-store [store fetch execute!]
      (is (= :ok (execute! fixture/publish-course!)))
      (is (= :ok (execute! (commands/init! section-test-id section-id course-id))))

      (let [course (fetch course-id)]
        (dotimes [i 5]
          (let [section-test (fetch section-test-id)]

            (is (= (:streak-length section-test) i))
            (let [question (course/question-for-section course section-id (:current-question-id section-test))]

              (is (= :ok (execute! (commands/check-answer! section-test-id section-id course-id (:id question)
                                                           (random-correct-input question))))))

            (let [correct (fetch section-test-id)]
              (is (= (:current-question-status correct) :answered-correctly))
              (is (= (:streak-length correct) (inc i))))

            (= :ok (execute! (commands/next-question! section-test-id section-id course-id)))))))))
