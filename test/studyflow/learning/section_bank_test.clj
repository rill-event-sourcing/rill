(ns studyflow.learning.section-bank-test
  (:require [studyflow.learning.section-bank :refer :all]
            [studyflow.learning.section-bank.events :as events]
            [studyflow.learning.section-test]
            [studyflow.learning.section-test.commands :refer [check-answer!]]
            [studyflow.learning.section-test.events :as section-test]
            [clojure.test :refer :all]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.course.fixture :as fixture]
            [studyflow.learning.course :as course]
            [rill.temp-store :refer [command-result= execute]]))

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


(deftest test-earnings

  (is (command-result= [:ok
                        [(section-test/question-answered-correctly section-id student-id question-id correct-inputs)]
                        [(events/coins-earned section-id student-id 3)]]
                       (execute (check-answer! section-id student-id 1 course-id question-id correct-inputs)
                                [fixture/course-published-event
                                 (section-test/created section-id student-id course-id)
                                 (section-test/question-assigned section-id student-id question-id 2)]))
      "get coins when answering correctly")

  (is (command-result= [:ok
                        [(section-test/question-answered-correctly section-id student-id question-id correct-inputs)]
                        []]
                       (execute (check-answer! section-id student-id 2 course-id question-id correct-inputs)
                                [fixture/course-published-event
                                 (section-test/created section-id student-id course-id)
                                 (section-test/question-assigned section-id student-id question-id 2)
                                 (section-test/question-answered-incorrectly section-id student-id question-id incorrect-inputs)]))
      "do not get coins when answering after incorrect answer")

  (is (command-result= [:ok
                        [(section-test/question-answered-correctly section-id student-id question-id correct-inputs)]
                        []]
                       (execute (check-answer! section-id student-id 2 course-id question-id correct-inputs)
                                [fixture/course-published-event
                                 (section-test/created section-id student-id course-id)
                                 (section-test/question-assigned section-id student-id question-id 2)
                                 (section-test/answer-revealed section-id student-id question-id correct-inputs)]))
      "do not get coins when answering after reveal answer")

  (is (command-result= [:ok
                        [(section-test/question-answered-correctly section-id student-id question-id correct-inputs)]
                        [(events/coins-earned section-id student-id 2)]]
                       (execute (check-answer! section-id student-id 1 course-id question-id correct-inputs)
                                [fixture/course-published-event
                                 (events/coins-earned section-id student-id 58)
                                 (section-test/created section-id student-id course-id)
                                 (section-test/question-assigned section-id student-id question-id 2)]))
      "get up to 60 coins out of a section test")

  (is (command-result= [:ok
                        [(section-test/question-answered-correctly section-id student-id question-id correct-inputs)]
                        []]
                       (execute (check-answer! section-id student-id 1 course-id question-id correct-inputs)
                                [fixture/course-published-event
                                 (events/coins-earned section-id student-id 60)
                                 (section-test/created section-id student-id course-id)
                                 (section-test/question-assigned section-id student-id question-id 2)]))
      "don't get more coins after the limit"))


