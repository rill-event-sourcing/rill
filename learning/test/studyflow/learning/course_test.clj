(ns studyflow.learning.course-test
  (:require [studyflow.learning.course :as course]
            [studyflow.learning.course.events :as events]
            [studyflow.learning.course.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.event-store.memory :refer [memory-store]]
            [rill.uuid :refer [new-id]]
            [rill.aggregate :refer [load-aggregate handle-command]]
            [clojure.test :refer [deftest testing is]]))

(def course fixture/course-aggregate)
(def section-id #uuid "baaffea6-3094-4494-8071-87c2854fd26f")

(deftest test-course-aggregate
  (is (= 1 (count (:chapters course))))
  (is (= (:id course) (:id fixture/course-edn)))
  (is (= (:id (course/section course section-id))
         section-id))
  (is (= 2 (count (course/questions-for-section course section-id))))
  (doseq [question-id (map :id (course/questions-for-section course section-id))]
    (let [question (course/question-for-section course section-id question-id)]
      (is question)
      (is (= (:id question) question-id)))))

(def new-course-id (new-id))
(deftest test-write-model-events
  (is (= (:id (load-aggregate [(events/published new-course-id {})]))
         new-course-id)))

(deftest test-try-command
  (let [store (memory-store)
        [status events] (try-command store (commands/publish! (:id fixture/course-edn) fixture/course-edn))]
    (is (= :ok status))
    (is (= [::events/Published] (map message/type events)))))

(deftest test-command-handler
  (testing "Publishing commands"
    (is (= (map message/type (second (handle-command nil (commands/publish! (:id fixture/course-edn) fixture/course-edn))))
           [::events/Published]))))

(deftest test-answer-checking
  (testing "Needs an answer for each input"
    (is (not (course/line-input-fields-answers-correct? [{:name "_INPUT_1_"
                                                          :correct-answers #{"1"}}
                                                         {:name "_INPUT_2_"
                                                          :correct-answers #{"2"}}]
                                                        {"_INPUT_1_" "1"}))))
  (testing "Don't consider leading or trailing whitespace for answer checking"
    (is (course/line-input-fields-answers-correct? [{:name "_INPUT_1_"
                                                     :correct-answers #{"111"}}
                                                    {:name "_INPUT_2_"
                                                     :correct-answers #{" 22 2 "}}
                                                    {:name "_INPUT_3_"
                                                     :correct-answers #{"333" "3333333"}}]
                                                   {"_INPUT_1_" "  111   "
                                                    "_INPUT_2_" "22 2"
                                                    "_INPUT_3_" "333"}))))
