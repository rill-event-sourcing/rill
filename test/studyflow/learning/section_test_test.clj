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

(def chapter-id   #uuid "8112d048-1189-4b45-a7ba-78da2b0c389d")
(def section-id   #uuid "baaffea6-3094-4494-8071-87c2854fd26f")
(def question-id  #uuid "4302505c-5498-4229-b11f-2da3aa869793")
(def question2-id #uuid "9bcbb97b-7935-420e-8ba7-fb4650de569f")

(def question-total 2)
(def correct-inputs  {"_INPUT_1_" "42"
                      "_INPUT_2_" "3"})
(def incorrect-inputs {"_INPUT_1_" "not 42"
                       "_INPUT_2_" "not 3"})

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
      (let [inputs correct-inputs]
        (is (command-result= [:ok [(events/question-answered-correctly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)])))))

    (testing "with a correct answer with extra whitespace"
      (let [inputs (update-in correct-inputs ["_INPUT_1_"] (fn [v] (str "   " v "  ")))]
        (is (command-result= [:ok [(events/question-answered-correctly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)])))))

    (testing "after answering expect to get other question"
      (is (command-result= [:ok [(events/question-assigned section-id student-id question2-id question-total)]]
                           (execute (commands/next-question! section-id student-id 2 course-id)
                                    [fixture/course-published-event
                                     (events/created section-id student-id course-id)
                                     (events/question-assigned section-id student-id question-id question-total)
                                     (events/question-answered-correctly section-id student-id question-id nil)]))))

    (testing "with incomplete answers"
      (let [inputs (dissoc correct-inputs "_INPUT_1_")]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)]))))

      (let [inputs {"_INPUT_2_" "not a correct answer"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)])))))

    (testing "with an incorrect answer"
      (let [inputs (assoc correct-inputs "_INPUT_2_" "not a correct answer")]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)]))))


      (let [inputs {"_INPUT_1_" "not correct at all"
                    "_INPUT_2_" "not even close"}]
        (is (command-result= [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]
                             (execute (commands/check-answer! section-id student-id 1 course-id question-id inputs)
                                      [fixture/course-published-event
                                       (events/created section-id student-id course-id)
                                       (events/question-assigned section-id student-id question-id question-total)])))))

    (testing "next question"
      (testing "with a correct answer"
        (let [inputs correct-inputs]
          (let [[status [event]] (execute (commands/next-question! section-id student-id 2 course-id)
                                          [fixture/course-published-event
                                           (events/created section-id student-id course-id)
                                           (events/question-assigned section-id student-id question-id question-total)
                                           (events/question-answered-correctly section-id student-id question-id inputs)])]
            (is (= :ok status)
                (= ::events/QuestionAssigned
                   (message/type event))))))

      (testing "with an incorrect answer"
        (let [inputs {"_INPUT_1_" "completely incorrect"}]
          (is (thrown? AssertionError
                       (execute (commands/next-question! section-id student-id 2 course-id)
                                [fixture/course-published-event
                                 (events/created section-id student-id course-id)
                                 (events/question-assigned section-id student-id question-id question-total)
                                 (events/question-answered-incorrectly section-id student-id question-id inputs)]))))))))


(deftest test-continue-practice
  (testing "the first streaks marks a section as finished, afterward you can continue practising and completing streaks"
    (testing "first five in a row correctly mark section as finished"
      (let [upto-fifth-q-stream
            (-> [fixture/course-published-event
                 (events/created section-id student-id course-id)]
                (into (reduce into []
                              (repeat 4 [(events/question-assigned section-id student-id question-id question-total)
                                         (events/question-answered-correctly section-id student-id question-id correct-inputs)])))
                (conj (events/question-assigned section-id student-id question-id question-total)))
            [status [correctly-answered-event finished-event :as events]]
            (execute (commands/check-answer! section-id student-id 9 course-id question-id correct-inputs)
                     upto-fifth-q-stream)]
        (is (= :ok status))
        (is (= (message/type correctly-answered-event)
               ::events/QuestionAnsweredCorrectly))
        (is (= (message/type finished-event)
               ::events/Finished))
        (testing "Finished events include chapter-id so we can relate this event to the chapter quiz"
          (is (= (:chapter-id finished-event) chapter-id)))
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
                                        (events/question-assigned section-id student-id question-id question-total))
                  [status [answered-correctly-event :as events]]
                  (execute (commands/check-answer! section-id student-id 12 course-id question-id correct-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-correctly-event)
                     ::events/QuestionAnsweredCorrectly))
              (is (= (count events) 1))))
          (testing "after a finished section-test you do not get stuck after 3 incorrect questions"
            (let [continue-stream (-> finished-stream
                                      (into (reduce into []
                                                    (repeat 2 [(events/question-assigned section-id student-id question-id question-total)
                                                               (events/question-answered-incorrectly section-id student-id question-id incorrect-inputs)
                                                               (events/question-answered-correctly section-id student-id question-id correct-inputs)])) )
                                      (conj (events/question-assigned section-id student-id question-id question-total)))
                  [status [answered-incorrectly-event :as events]]
                  (execute (commands/check-answer! section-id student-id 18 course-id question-id incorrect-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-incorrectly-event)
                     ::events/QuestionAnsweredIncorrectly))
              (is (= (count events) 1))))

          (testing "after finished for another streak a StreakCompleted is generated"
            (let [continue-stream (into finished-stream
                                        (interpose
                                         (events/question-answered-correctly section-id student-id question-id correct-inputs)
                                         (repeat 5 (events/question-assigned section-id student-id question-id question-total))))
                  [status [answered-correctly-event streak-event :as events]]
                  (execute (commands/check-answer! section-id student-id 20 course-id question-id correct-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-correctly-event)
                     ::events/QuestionAnsweredCorrectly))
              (is (= (message/type streak-event)
                     ::events/StreakCompleted))))
          (testing "after a StreakCompleted for another streak a StreakCompleted is generated"
            (let [continue-stream (-> finished-stream
                                      (into (interleave
                                             (repeat 5 (events/question-assigned section-id student-id question-id question-total))
                                             (repeat 5 (events/question-answered-correctly section-id student-id question-id correct-inputs))))
                                      (conj (events/streak-completed section-id student-id))
                                      (into (interpose
                                             (events/question-answered-correctly section-id student-id question-id correct-inputs)
                                             (repeat 5 (events/question-assigned section-id student-id question-id question-total)))))
                  [status [answered-correctly-event streak-event :as events]]
                  (execute (commands/check-answer! section-id student-id 31 course-id question-id correct-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-correctly-event)
                     ::events/QuestionAnsweredCorrectly))
              (is (= (message/type streak-event)
                     ::events/StreakCompleted))))
          (testing "after a wrong answer and then another streak a StreakCompleted is generated"
            (let [continue-stream (-> finished-stream
                                      (into [(events/question-assigned section-id student-id question-id question-total)
                                             (events/question-answered-incorrectly section-id student-id question-id correct-inputs)])
                                      (into (interpose
                                             (events/question-answered-correctly section-id student-id question-id correct-inputs)
                                             (repeat 5 (events/question-assigned section-id student-id question-id question-total)))))
                  [status [answered-correctly-event streak-event :as events]]
                  (execute (commands/check-answer! section-id student-id 22 course-id question-id correct-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-correctly-event)
                     ::events/QuestionAnsweredCorrectly))
              (is (= (message/type streak-event)
                     ::events/StreakCompleted))
              )))))
    (testing "three in a row make you stumble"
      (let [upto-third-q-stream
            (-> [fixture/course-published-event
                 (events/created section-id student-id course-id)]
                (into (reduce into []
                              (repeat 2 [(events/question-assigned section-id student-id question-id question-total)
                                         (events/question-answered-incorrectly section-id student-id question-id incorrect-inputs)
                                         (events/question-answered-correctly section-id student-id question-id correct-inputs)])))
                (conj (events/question-assigned section-id student-id question-id question-total)))
            [status [answered-incorrectly-event stuck-event :as events]]
            (execute (commands/check-answer! section-id student-id 7 course-id question-id incorrect-inputs)
                     upto-third-q-stream)]
        (is (= :ok status))
        (is (= (message/type answered-incorrectly-event)
               ::events/QuestionAnsweredIncorrectly))
        (is (= (message/type stuck-event)
               ::events/Stuck))
        (let [stuck-stream (-> (into upto-third-q-stream events)
                               (into [(events/question-answered-correctly section-id student-id question-id correct-inputs)]))]
          (testing "after a stuck section-test you can continue"
            (let [[status [assigned-event :as events]]
                  (execute (commands/next-question! section-id student-id 10 course-id)
                           stuck-stream)]
              (is (= :ok status))
              (is (= (message/type assigned-event)
                     ::events/QuestionAssigned))))
          (testing "the fourth incorrect answer after a stuck won't generate a Stuck"
            (let [continue-stream
                  (conj stuck-stream
                        (events/question-assigned section-id student-id question-id question-total))
                  [status [answered-incorrectly-event :as events]]
                  (execute (commands/check-answer! section-id student-id 11 course-id question-id incorrect-inputs)
                           continue-stream)]
              (is (= status :ok))
              (is (= (message/type answered-incorrectly-event)
                     ::events/QuestionAnsweredIncorrectly))
              (is (= (count events) 1))))
          (testing "the first correct answer after a stuck, will unstuck"
            (let [continue-after-stuck-stream
                  (conj stuck-stream
                        (events/question-assigned section-id student-id question-id question-total))
                  [status [answered-correctly-event unstuck-event :as events]]
                  (execute (commands/check-answer! section-id student-id 11 course-id question-id correct-inputs)
                           continue-after-stuck-stream)]
              (is (= status :ok))
              (is (= (message/type answered-correctly-event)
                     ::events/QuestionAnsweredCorrectly))
              (is (= (message/type unstuck-event)
                     ::events/Unstuck))
              (let [stuck-then-unstuck-stream (->
                                               (into continue-after-stuck-stream events)
                                               (into (reduce into []
                                                             (repeat 2 [(events/question-assigned section-id student-id question-id question-total)
                                                                        (events/question-answered-incorrectly section-id student-id question-id incorrect-inputs)
                                                                        (events/question-answered-correctly section-id student-id question-id correct-inputs)])) )
                                               (conj (events/question-assigned section-id student-id question-id question-total)))]

                (testing "getting stuck again after being unstuck"
                  (let [[status [answered-incorrectly-event stuck-event :as events]]
                        (execute (commands/check-answer! section-id student-id 20 course-id question-id incorrect-inputs)
                                 stuck-then-unstuck-stream)]
                    (is (= :ok status))
                    (is (= (message/type answered-incorrectly-event)
                           ::events/QuestionAnsweredIncorrectly))
                    (is (= (message/type stuck-event)
                           ::events/Stuck)))))))))))

  (deftest test-reveal-worked-out-answer
    (let [answers {"_INPUT_1_" "doesn't matter"
                   "_INPUT_2_" "doesn't matter"}
          first-question-stream
          [fixture/course-published-event
           (events/created section-id student-id course-id)
           (events/question-assigned section-id student-id question-id question-total)]
          [status [revealed-event :as events]]
          (execute (commands/reveal-answer! section-id student-id 1 course-id question-id)
                   first-question-stream)]
      (testing "can request answer for assigned question"
        (is (= :ok status))
        (is (= (message/type revealed-event)
               ::events/AnswerRevealed))
        (is (= (count events) 1)))
      (let [inputs {"_INPUT_1_" "6"
                    "_INPUT_2_" "correct"}
            answered-stream (conj first-question-stream
                                  (events/question-answered-correctly section-id student-id question-id inputs))]
        (testing "can ask for answer for answered question"
          (let [[status [revealed-event :as events]]
                (execute (commands/reveal-answer! section-id student-id 2 course-id question-id)
                         answered-stream)]
            (is (= :ok status))
            (is (= (message/type revealed-event)
                   ::events/AnswerRevealed))
            (is (= (count events) 1)))))
      (testing "can ask for answer after answering incorrectly"
        (let [inputs {"_INPUT_1_" "wrong"
                      "_INPUT_2_" "wrong"}
              answered-stream (conj first-question-stream
                                    (events/question-answered-incorrectly section-id student-id question-id inputs))
              [status [revealed-event :as events]]
              (execute (commands/reveal-answer! section-id student-id 2 course-id question-id)
                       answered-stream)]
          (is (= :ok status))
          (is (= (message/type revealed-event)
                 ::events/AnswerRevealed))
          (is (= (count events) 1))))
      (testing "cannot ask for answer after it has been revealed already"
        (let [revealed-stream (conj first-question-stream
                                    (events/answer-revealed section-id student-id question-id answers))]
          (is (thrown? AssertionError
                       (execute (commands/reveal-answer! section-id student-id 2 course-id question-id)
                                revealed-stream))))))))
