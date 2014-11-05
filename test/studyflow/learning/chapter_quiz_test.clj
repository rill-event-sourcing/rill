(ns studyflow.learning.chapter-quiz-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.chapter-quiz :as chapter-quiz]
            [studyflow.learning.chapter-quiz.events :as events]
            [studyflow.learning.section-test.commands :as section-test]
            [studyflow.learning.section-test.events :as section-test-events]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [studyflow.rand :refer [*rand-nth*]]
            [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
            [clojure.test :refer [deftest testing is]]))

(def chapter-id #uuid "8112d048-1189-4b45-a7ba-78da2b0c389d")
(def question-set-1-id #uuid "24380676-6d1c-4a31-906b-533878270a9b")

(def question-id #uuid "24f80676-6d1c-4a31-906b-533878270a9b")

(def question-set-2-id #uuid "aa4195a3-4f31-43bb-99d2-a14ef6acf426")
(def question-2-id #uuid "50ee5d38-86c6-4297-aaea-9148e067d8bc")

(def question-set-3-id #uuid "32c63461-5fd2-45a7-b123-ddcfb869eb32")
(def question-3-id #uuid "28af4894-e20d-4fad-bdd8-957ab20d7a2c")

(def question-set-4-id #uuid "df9cd899-54e4-486d-b2f8-14ecc6cee79c")
(def question-4-id #uuid "600137bd-f95c-4c41-b30c-41ef14999759")

(def section-ids (map :id (:sections (course/chapter fixture/course-edn chapter-id))))

(def correct-inputs {"_INPUT_1_" "2"
                     "_INPUT_2_" "2"})

(def incorrect-inputs {"_INPUT_1_" "not 2"
                       "_INPUT_2_" "not 2"})
(def correct-inputs-by-id {#uuid "24f80676-6d1c-4a31-906b-533878270a9b" {"_INPUT_1_" "2"  ; question-id
                                                                         "_INPUT_2_" "2"}
                           #uuid "50ee5d38-86c6-4297-aaea-9148e067d8bc" {"_INPUT_1_" "3"  ; question-2-id
                                                                         "_INPUT_2_" "1"}
                           #uuid "28af4894-e20d-4fad-bdd8-957ab20d7a2c" {"_INPUT_1_" "12" ; question-3-id
                                                                         "_INPUT_2_" "10"}
                           #uuid "600137bd-f95c-4c41-b30c-41ef14999759" {"_INPUT_1_" "42" ; question-4-id
                                                                         "_INPUT_2_" "101"}})

(def course fixture/course-aggregate)
(def course-id (:id course))
(def student-id (new-id))

(defn test-rand-question [question-set]
  (first (sort-by :id question-set)))

(deftest test-commands
  (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id question-id))]
    (testing "starting chapter-quiz"
      (is (command-result= [:ok [(events/started course-id chapter-id student-id true)
                                 (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)]]
                           (execute (chapter-quiz/start! course-id chapter-id student-id)
                                    [fixture/course-published-event])))))

  (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id question-2-id))]
    (testing "submitting an answer"
      (testing "with a correct answer"
        (let [inputs correct-inputs]
          (is (command-result= [:ok [(events/question-answered-correctly course-id chapter-id student-id question-id inputs)
                                     (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)]]
                               (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-id 1 inputs)
                                        [fixture/course-published-event
                                         (events/started course-id chapter-id student-id true)
                                         (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)])))))
      (testing "with an incorrect answer"
        (let [inputs incorrect-inputs]
          (is (command-result= [:ok [(events/question-answered-incorrectly course-id chapter-id student-id question-id inputs)]]
                               (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-id 1 inputs)
                                        [fixture/course-published-event
                                         (events/started course-id chapter-id student-id true)
                                         (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)])))))

      (testing "dismissing the first error"
        (is (command-result= [:ok [(events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)]]
                             (execute (chapter-quiz/dismiss-error-screen! course-id chapter-id student-id)
                                      [fixture/course-published-event
                                       (events/started course-id chapter-id student-id true)
                                       (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                       (events/question-answered-incorrectly course-id chapter-id student-id question-id incorrect-inputs)]))))
      (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id question-3-id))]
        (testing "an incorrect and then correct answer"
          (is (command-result= [:ok [(events/question-answered-correctly course-id chapter-id student-id question-2-id (correct-inputs-by-id question-2-id))
                                     (events/question-assigned course-id chapter-id student-id question-set-3-id question-3-id)]]
                               (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-2-id 3 (correct-inputs-by-id question-2-id))
                                        [fixture/course-published-event
                                         (events/started course-id chapter-id student-id true)
                                         (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                         (events/question-answered-incorrectly course-id chapter-id student-id question-id incorrect-inputs)
                                         (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)])))))
      (testing "two incorrect answers"
        (is (command-result= [:ok [(events/question-answered-incorrectly course-id chapter-id student-id question-2-id (correct-inputs-by-id question-id))]]
                             (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-2-id 3 (correct-inputs-by-id question-id))
                                      [fixture/course-published-event
                                       (events/started course-id chapter-id student-id true)
                                       (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                       (events/question-answered-incorrectly course-id chapter-id student-id question-id incorrect-inputs)
                                       (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)]))))

      (testing "two incorrect answers and dismissed error"
        (is (command-result= [:ok [(events/failed course-id chapter-id student-id)
                                   (events/locked course-id chapter-id student-id)]]
                             (execute (chapter-quiz/dismiss-error-screen! course-id chapter-id student-id)
                                      [fixture/course-published-event
                                       (events/started course-id chapter-id student-id true)
                                       (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                       (events/question-answered-incorrectly course-id chapter-id student-id question-id incorrect-inputs)
                                       (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)
                                       (events/question-answered-incorrectly course-id chapter-id student-id question-2-id (correct-inputs-by-id question-id))]))))

      (testing "four correct answers (and a pass) in a row"
        (is (command-result= [:ok [(events/question-answered-correctly course-id chapter-id student-id question-4-id (correct-inputs-by-id question-4-id))
                                   (events/passed course-id chapter-id student-id)
                                   ]]
                             (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-4-id 7 (correct-inputs-by-id question-4-id))
                                      [fixture/course-published-event
                                       (events/started course-id chapter-id student-id true)
                                       (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                       (events/question-answered-correctly course-id chapter-id student-id question-id (correct-inputs-by-id question-id))
                                       (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)
                                       (events/question-answered-correctly course-id chapter-id student-id question-2-id (correct-inputs-by-id question-2-id))
                                       (events/question-assigned course-id chapter-id student-id question-set-3-id question-3-id)
                                       (events/question-answered-correctly course-id chapter-id student-id question-3-id (correct-inputs-by-id question-3-id))
                                       (events/question-assigned course-id chapter-id student-id question-set-4-id question-4-id)

])))))))


(def section-question (first (:questions (second (:sections (course/chapter fixture/course-edn chapter-id))))))
(def section-question-input  {"_INPUT_1_" (-> section-question :line-input-fields first :correct-answers first)})

(deftest test-unlocking
  (testing "unlocking the chapter-quiz"
    (is (command-result= [:ok
                          [(section-test-events/question-answered-correctly (second section-ids) student-id (:id section-question) section-question-input)
                           (section-test-events/finished (second section-ids) student-id chapter-id course-id)]
                          [(events/section-finished course-id chapter-id student-id (second section-ids) (set section-ids))
                           (events/un-locked course-id chapter-id student-id)]]
                         (execute (section-test/check-answer! (second section-ids) student-id 9 course-id (:id section-question) section-question-input)
                                  [fixture/course-published-event
                                   (events/section-finished course-id chapter-id student-id (first section-ids) (set section-ids))
                                   (section-test-events/created (second section-ids) student-id course-id)
                                   (section-test-events/question-assigned (second section-ids) student-id 1 10)
                                   (section-test-events/question-answered-correctly (second section-ids) student-id 1 {:foo :bar})
                                   (section-test-events/question-assigned (second section-ids) student-id 2 10)
                                   (section-test-events/question-answered-correctly (second section-ids) student-id 2 {:foo :bar})
                                   (section-test-events/question-assigned (second section-ids) student-id 3 10)
                                   (section-test-events/question-answered-correctly (second section-ids) student-id 3 {:foo :bar})
                                   (section-test-events/question-assigned (second section-ids) student-id 4 10)
                                   (section-test-events/question-answered-correctly (second section-ids) student-id 4 {:foo :bar})
                                   (section-test-events/question-assigned (second section-ids) student-id (:id section-question) 10)]))))

  (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id question-id))]
    (testing "starting the chapter-quiz after being unlocked without being locked"
      (is (command-result= [:ok [(events/started course-id chapter-id student-id false)
                                 (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)]]
                           (execute (chapter-quiz/start! course-id chapter-id student-id)
                                    [fixture/course-published-event
                                     (events/section-finished course-id chapter-id student-id (first section-ids) (set section-ids))
                                     (section-test-events/created (second section-ids) student-id course-id)
                                     (section-test-events/question-assigned (second section-ids) student-id 1 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 1 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 2 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 2 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 3 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 3 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 4 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 4 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id (:id section-question) 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id (:id section-question) section-question-input)
                                     (section-test-events/finished (second section-ids) student-id chapter-id course-id)
                                     (events/section-finished course-id chapter-id student-id (second section-ids) (set section-ids))
                                     (events/un-locked course-id chapter-id student-id)]))))
    (testing "failing the fast-route, unlocking and starting the chapter-quiz"
      (is (command-result= [:ok [(events/started course-id chapter-id student-id false)
                                 (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)]]
                           (execute (chapter-quiz/start! course-id chapter-id student-id)
                                    [fixture/course-published-event
                                     (events/started course-id chapter-id student-id true)
                                     (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)
                                     (events/question-answered-incorrectly course-id chapter-id student-id question-id incorrect-inputs)
                                     (events/question-assigned course-id chapter-id student-id question-set-2-id question-2-id)
                                     (events/question-answered-incorrectly course-id chapter-id student-id question-2-id (correct-inputs-by-id question-id))
                                     (events/failed course-id chapter-id student-id)
                                     (events/locked course-id chapter-id student-id)
                                     (events/section-finished course-id chapter-id student-id (first section-ids) (set section-ids))
                                     (section-test-events/created (second section-ids) student-id course-id)
                                     (section-test-events/question-assigned (second section-ids) student-id 1 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 1 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 2 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 2 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 3 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 3 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id 4 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id 4 {:foo :bar})
                                     (section-test-events/question-assigned (second section-ids) student-id (:id section-question) 10)
                                     (section-test-events/question-answered-correctly (second section-ids) student-id (:id section-question) section-question-input)
                                     (section-test-events/finished (second section-ids) student-id chapter-id course-id)
                                     (events/section-finished course-id chapter-id student-id (second section-ids) (set section-ids))
                                     (events/un-locked course-id chapter-id student-id)]))))))
