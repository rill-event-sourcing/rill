(ns studyflow.learning.section-test-test
  (:require [studyflow.learning.section-test :as section-test]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course.fixture :as fixture]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [rill.aggregate :refer [handle-event handle-command]]
            [studyflow.learning.course-test :refer [course section-id]]
            [clojure.test :refer [deftest testing is]]))

(def section-test-id (new-id))

(deftest test-section-test-aggregate
  (is (= (map message/type (handle-command nil
                                           (commands/->Init! (new-id) section-test-id section-id (:id course))
                                           fixture/course-edn))
         [::events/Created ::events/QuestionAssigned])))
