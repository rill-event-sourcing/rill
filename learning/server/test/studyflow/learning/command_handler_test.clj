(ns studyflow.learning.command-handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [studyflow.events :as evt]
            [studyflow.learning.commands :as cmd]
            [studyflow.learning.command-handler]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id]]
            [rill.event-store.memory :refer [memory-store]]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :refer [read-example-json]]))

(deftest test-command-handler
  (let [store (memory-store)
        input (material/parse-course-material (read-example-json))]
    (try-command store (cmd/->PublishCourse! (new-id) (:id input) input))))

