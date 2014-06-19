(ns studyflow.learning.write-model-test
  (:require [studyflow.learning.write-model :as model]
            [rill.aggregate :refer [load-aggregate]]
            [studyflow.events :refer [->CourseUpdated ->CoursePublished]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest is]]))

(def course-id (new-id))

(deftest test-write-model-events
  (is (= (:id (load-aggregate [(->CoursePublished (new-id) course-id {})]))
         course-id)))

