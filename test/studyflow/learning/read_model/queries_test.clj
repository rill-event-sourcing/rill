(ns studyflow.learning.read-model.queries-test
  (:require [studyflow.learning.read-model.queries :as q]
            [studyflow.learning.read-model :as m]
            [studyflow.learning.course-material :as material]
            [clojure.test :refer [deftest is testing]]
            [studyflow.learning.course-material-test :refer [read-example-json]]))

(def material (material/parse-course-material (read-example-json)))
(def course-id (:id material))
(def model (m/set-course m/empty-model course-id material))

(deftest test-queries
  (let [tree (q/course-material model course-id nil)]
    (is (= (:name tree)
           "Counting"))
    (is (vector? (:chapters tree)))
    (is (vector? (-> tree :chapters first :sections)))
    (is (= (set (keys (-> tree :chapters first :sections first)))
           #{:id :title :status}))))


