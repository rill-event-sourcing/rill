(ns studyflow.learning.command-handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [studyflow.events :as evt]
            [studyflow.learning.commands :as cmd]
            [studyflow.learning.command-handler]
            [rill.handler :refer [try-command handle-command]]
            [rill.uuid :refer [new-id]]
            [rill.event-store.memory :refer [memory-store]]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :refer [read-example-json]]))

(def initial-material (material/parse-course-material (read-example-json)))

(deftest test-try-command
  (let [store (memory-store)]
    (is (= (try-command store (cmd/->PublishCourse! (new-id) (:id initial-material) initial-material))
           :ok))
    (is (= (try-command store (cmd/->PublishCourse! (new-id) (:id initial-material) initial-material))
           :ok))))

(deftest test-command-handler
  (testing "Publishing commands"
    (is (= (map class (handle-command (cmd/->PublishCourse! (new-id) (:id initial-material) initial-material) nil))
           [studyflow.events.CoursePublished]))))
