(ns studyflow.learning.chapter-quiz-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.chapter-quiz :as chapter-quiz]
            [studyflow.learning.chapter-quiz.events :as events]
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
(def qs-1-question-1-id #uuid "24f80676-6d1c-4a31-906b-533878270a9b")
(def qs-1-question-2-id #uuid "3a4195a3-4f31-43bb-99d2-a14ef6acf426")

(def question-set-2-id #uuid "aa4195a3-4f31-43bb-99d2-a14ef6acf426")
(def qs-2-question-1-id #uuid "50ee5d38-86c6-4297-aaea-9148e067d8bc")
(def qs-2-question-2-id #uuid "f02d81f7-df3f-48e2-a5d9-fc4094c8bf14")

(def question-id qs-1-question-1-id)

(def correct-inputs {"_INPUT_1_" "2"
                     "_INPUT_2_" "2"})

(def incorrect-inputs {"_INPUT_1_" "not 2"
                       "_INPUT_2_" "not 2"})

(def course fixture/course-aggregate)
(def course-id (:id course))
(def student-id (new-id))

(defn test-rand-question [question-set]
  (first (sort-by :id question-set)))

(deftest test-commands
  (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id question-id))]
    (testing "starting chapter-quiz"
      (is (command-result= [:ok [(events/started course-id chapter-id student-id)
                                 (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)]]
                           (execute (chapter-quiz/start! course-id chapter-id student-id)
                                    [fixture/course-published-event])))))

  (binding [*rand-nth* (constantly (course/question-for-chapter-quiz course chapter-id qs-2-question-1-id))]
    (testing "submitting an answer"
      (testing "with a correct answer"
        (let [inputs correct-inputs]
          (is (command-result= [:ok [(events/question-answered-correctly course-id chapter-id student-id question-id inputs)
                                     (events/question-assigned course-id chapter-id student-id question-set-2-id qs-2-question-1-id)]]
                               (execute (chapter-quiz/submit-answer! course-id chapter-id student-id question-id 1 inputs)
                                        [fixture/course-published-event
                                         (events/started course-id chapter-id student-id)
                                         (events/question-assigned course-id chapter-id student-id question-set-1-id question-id)])))))))
  )
