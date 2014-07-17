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
(def section-id #uuid "9a40bd5f-e824-4454-9d0c-785cbf56392b")

(deftest test-course-aggregate
  (is (= (count (:chapters course))
         1))
  (is (= (:id course) (:id fixture/course-edn)))
  (is (= (:id (course/section course section-id))
         section-id))
  (is (= (count (course/questions-for-section course section-id))
         2))
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
    (is (= (map message/type (handle-command nil (commands/publish! (:id fixture/course-edn) fixture/course-edn)))
           [::events/Published]))))
