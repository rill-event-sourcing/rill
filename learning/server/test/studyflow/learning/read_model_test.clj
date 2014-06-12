(ns studyflow.learning.read-model-test
  (:require [clojure.test :refer [is deftest testing]]
            [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.read-model.event-handler :as handler]
            [studyflow.events :as events]
            [rill.uuid :refer [new-id]]))

(deftest read-model-test
  (testing "Chapters and learning steps can be requested as a tree"
    (is (= (get-in (model/course-tree (fixture/read-example-json)) [:chapters 1 :sections 2 :title])
           "Vermenigvuldigen"))

    (is (= (set (keys (get-in (model/course-tree (fixture/read-example-json)) [:chapters 1 :sections 2])))
           #{:title :id})
        "no subsection data in the course tree")))



