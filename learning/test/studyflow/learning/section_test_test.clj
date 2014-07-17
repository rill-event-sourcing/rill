(ns studyflow.learning.section-test-test
  (:require [studyflow.learning.section-test :as section-test]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def section-test-id (new-id))
(def section-id #uuid "9a40bd5f-e824-4454-9d0c-785cbf56392b")
(def question-id #uuid "ef67f401-14cc-41d0-8305-c2a1392b8639")

(def course fixture/course-aggregate)
(def course-id (:id course))

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

            (= :ok (execute! (commands/next-question! section-test-id section-id course-id))))))))


  (testing "eventually correct flow"
    (with-temp-store [store fetch execute!]
      (is (= :ok (execute! fixture/publish-course!)))
      (is (= :ok (execute! (commands/init! section-test-id section-id course-id))))

      (let [course (fetch course-id)
            goto-next! #(execute! (commands/next-question! section-test-id section-id course-id))
            check-with (fn [gen-input]
                         (let [section-test (fetch section-test-id)
                               question (course/question-for-section course section-id (:current-question-id section-test))]
                           (execute! (commands/check-answer! section-test-id section-id course-id (:id question) (gen-input question)))))
            check-correct! #(check-with random-correct-input)
            check-incorrect! #(check-with random-incorrect-input)]

        (check-correct!)
        (is (= 1 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (is (not (:finished? (fetch section-test-id))))
        (is (= 2 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (goto-next!)
        (is (= 3 (:streak-length (fetch section-test-id))))
        (check-incorrect!)
        (is (not (:finished? (fetch section-test-id))))
        (is (= 0 (:streak-length (fetch section-test-id))))
        (check-correct!)
        (is (= 0 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (is (= 1 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (is (not (:finished? (fetch section-test-id))))
        (is (= 2 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (is (= 3 (:streak-length (fetch section-test-id))))
        (goto-next!)
        (check-correct!)
        (goto-next!)
        (is (= 4 (:streak-length (fetch section-test-id))))
        (is (not (:finished? (fetch section-test-id))))
        (check-correct!)
        (is (= 5 (:streak-length (fetch section-test-id))))
        (is (:finished? (fetch section-test-id)))))))

(deftest test-commands
  (testing "init section test"
    (let [[status [event1 event2]] (execute (commands/init! section-test-id section-id course-id)
                                            [fixture/course-published-event])]
      (is (= :ok status))
      (is (message= (events/created section-test-id course-id section-id)
                    event1))
      (is (= ::events/QuestionAssigned
             (message/type event2)))))

  (testing "answering questions"
    (testing "correct answer"
      (let [inputs {"_INPUT_1_" "6"}
            question-id #uuid "ef67f401-14cc-41d0-8305-c2a1392b8639"]
        (is (command-result= [:ok [(events/question-answered-correctly section-test-id question-id inputs)]]
                             (execute (commands/check-answer! section-test-id section-id course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-test-id course-id section-id)
                                       (events/question-assigned section-test-id course-id question-id)])))))

    (testing "incorrect answer"
      (let [inputs {"_INPUT_1_" "7"}
            question-id #uuid "ef67f401-14cc-41d0-8305-c2a1392b8639"]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-test-id question-id inputs)]]
                             (execute (commands/check-answer! section-test-id section-id course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-test-id course-id section-id)
                                       (events/question-assigned section-test-id course-id question-id)])))))

    (testing "next question"
      (testing "with a correct answer"
        (let [inputs {"_INPUT_1_" "6"}
              question-id #uuid "ef67f401-14cc-41d0-8305-c2a1392b8639"]
          (let [[status [event]] (execute (commands/next-question! section-test-id section-id course-id)
                                          [fixture/course-published-event
                                           (events/created section-test-id course-id section-id)
                                           (events/question-assigned section-test-id course-id question-id)
                                           (events/question-answered-correctly section-test-id question-id inputs)])]
            (is (= :ok status)
                (= ::events/QuestionAssigned
                   (message/type event))))))

      (testing "with an incorrect answer"
        (let [inputs {"_INPUT_1_" "7"}
              question-id #uuid "ef67f401-14cc-41d0-8305-c2a1392b8639"]
          (is (thrown? AssertionError
                       (execute (commands/next-question! section-test-id section-id course-id)
                                [fixture/course-published-event
                                 (events/created section-test-id course-id section-id)
                                 (events/question-assigned section-test-id course-id question-id)
                                 (events/question-answered-incorrectly section-test-id question-id inputs)]))))))))
