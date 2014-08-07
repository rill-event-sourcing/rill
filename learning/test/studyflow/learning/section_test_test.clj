(ns studyflow.learning.section-test-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.section-test :as section-test]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def section-id #uuid  "baaffea6-3094-4494-8071-87c2854fd26f")
(def question-id #uuid "3e09e382-266c-4b16-9020-c5a071c2e2a4")

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
  (testing "init section test"
    (let [[status [event1 event2]] (execute (commands/init! section-id student-id course-id)
                                            [fixture/course-published-event])]
      (is (= :ok status))
      (is (message= (events/created section-id student-id course-id)
                    event1))
      (is (= ::events/QuestionAssigned
             (message/type event2)))))

  (testing "answering questions"
    (testing "with a correct answer"
      (let [inputs {"_INPUT_1_" "6"
                    "_INPUT_2_" "notcorrect"}] ;; UGH! somebody put that in as the only correct answer... :-(
        (is (command-result= [:ok [(events/question-answered-correctly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id)])))))

    (testing "with incomplete answers"
      (let [inputs {"_INPUT_1_" "7"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id)]))))

      (let [inputs {"_INPUT_1_" "8"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id)])))))

    (testing "with an incorrect answer"
      (let [inputs {"_INPUT_1_" "7"
                    "_INPUT_2_" "notcorrect"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id)]))))


      (let [inputs {"_INPUT_1_" "8"
                    "_INPUT_2_" "oasdkay"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id)])))))

    (testing "next question"
      (testing "with a correct answer"
        (let [inputs {"_INPUT_1_" "6"
                      "_INPUT_2_" "notcorrect"}]
          (let [[status [event]] (execute (commands/next-question! section-id student-id 2 course-id)
                                          [fixture/course-published-event
                                           (events/created section-id student-id course-id)
                                           (events/question-assigned section-id student-id question-id)
                                           (events/question-answered-correctly section-id student-id question-id inputs)])]
            (is (= :ok status)
                (= ::events/QuestionAssigned
                   (message/type event))))))

      (testing "with an incorrect answer"
        (let [inputs {"_INPUT_1_" "7"}]
          (is (thrown? AssertionError
                       (execute (commands/next-question! section-id student-id 2 course-id)
                                [fixture/course-published-event
                                 (events/created section-id student-id course-id)
                                 (events/question-assigned section-id student-id question-id)
                                 (events/question-answered-incorrectly section-id student-id question-id inputs)]))))))))


(deftest test-continue-practice
  (testing "the first streaks marks a section as finished, afterward you can continue practising and completing streaks"
    (let [inputs {"_INPUT_1_" "6"
                  "_INPUT_2_" "notcorrect"}] ;; notcorrect is actually
      ;; the correct answer
      (testing "first five in a row correctly mark section as finished"
        (let [upto-fifth-q-stream
              (-> [fixture/course-published-event
                   (events/created section-id student-id course-id)]
                  (into (reduce into []
                                (repeat 4 [(events/question-assigned section-id student-id question-id)
                                           (events/question-answered-correctly section-id student-id question-id inputs)])))
                  (conj (events/question-assigned section-id student-id question-id)))
              [status [correctly-answered-event finished-event :as events]]
              (execute (commands/check-answer! section-id student-id 9 course-id question-id inputs)
                       upto-fifth-q-stream)]
          (is (= :ok status))
          (is (= (message/type correctly-answered-event)
                 ::events/QuestionAnsweredCorrectly))
          (is (= (message/type finished-event)
                 ::events/Finished))
          (let [finished-stream (into upto-fifth-q-stream events)]
            (testing "after a finished section-test you can continue"
              (let [[status [assigned-event :as events]]
                    (execute (commands/next-question! section-id student-id 11 course-id)
                             finished-stream)]
                (is (= :ok status))
                (is (= (message/type assigned-event)
                       ::events/QuestionAssigned))))
            (testing "the sixth correct answer after a finish won't generate a StreakCompleted"
              (let [continue-stream (conj finished-stream
                                          (events/question-assigned section-id student-id question-id))
                    [status [answered-correctly-event :as events]]
                    (execute (commands/check-answer! section-id student-id 12 course-id question-id inputs)
                             continue-stream)]
                (is (= status :ok))
                (is (= (message/type answered-correctly-event)
                       ::events/QuestionAnsweredCorrectly))
                (is (= (count events) 1))))
            (testing "after finished for another streak a StreakCompleted is generated"
              (let [continue-stream (into finished-stream
                                          (interpose
                                           (events/question-answered-correctly section-id student-id question-id inputs)
                                           (repeat 5 (events/question-assigned section-id student-id question-id))))
                    [status [answered-correctly-event streak-event :as events]]
                    (execute (commands/check-answer! section-id student-id 20 course-id question-id inputs)
                             continue-stream)]
                (is (= status :ok))
                (is (= (message/type answered-correctly-event)
                       ::events/QuestionAnsweredCorrectly))
                (is (= (message/type streak-event)
                       ::events/StreakCompleted))))
            (testing "after a StreakCompleted for another streak a StreakCompleted is generated"
              (let [continue-stream (-> finished-stream
                                        (into (interleave
                                               (repeat 5 (events/question-assigned section-id student-id question-id))
                                               (repeat 5 (events/question-answered-correctly section-id student-id question-id inputs))))
                                        (conj (events/streak-completed section-id student-id))
                                        (into (interpose
                                               (events/question-answered-correctly section-id student-id question-id inputs)
                                               (repeat 5 (events/question-assigned section-id student-id question-id)))))
                    [status [answered-correctly-event streak-event :as events]]
                    (execute (commands/check-answer! section-id student-id 31 course-id question-id inputs)
                             continue-stream)]
                (is (= status :ok))
                (is (= (message/type answered-correctly-event)
                       ::events/QuestionAnsweredCorrectly))
                (is (= (message/type streak-event)
                       ::events/StreakCompleted))))
            (testing "after a wrong answer and then another streak a StreakCompleted is generated"
              (let [continue-stream (-> finished-stream
                                        (into [(events/question-assigned section-id student-id question-id)
                                               (events/question-answered-incorrectly section-id student-id question-id inputs)])
                                        (into (interpose
                                               (events/question-answered-correctly section-id student-id question-id inputs)
                                               (repeat 5 (events/question-assigned section-id student-id question-id)))))
                    [status [answered-correctly-event streak-event :as events]]
                    (execute (commands/check-answer! section-id student-id 22 course-id question-id inputs)
                             continue-stream)]
                (is (= status :ok))
                (is (= (message/type answered-correctly-event)
                       ::events/QuestionAnsweredCorrectly))
                (is (= (message/type streak-event)
                       ::events/StreakCompleted))
                ))))))))
