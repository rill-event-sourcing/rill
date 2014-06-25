(ns studyflow.learning.read-model-test
  (:require [clojure.test :refer [is deftest testing]]
            [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.read-model.event-handler :as handler]
            [studyflow.learning.course.events :as events]
            [rill.uuid :refer [new-id]]))

(def material (material/parse-course-material (fixture/read-example-json)))
(def course-id (:id material))
(def model (model/set-course nil course-id material))
(def section-id (:id (first (:sections (first (:chapters material))))))

(deftest read-model-test
  (testing "Chapters and learning steps can be requested as a tree"
    (is (= (get-in (model/course-tree (model/get-course model course-id)) [:chapters 1 :sections 2 :title])
           "Vermenigvuldigen"))

    (is (= (set (keys (get-in (model/course-tree (model/get-course model course-id)) [:chapters 1 :sections 2])))
           #{:title :id})
        "no subsection data in the course tree")

    (assert section-id)
    (is (= (:id (model/get-section (model/get-course model course-id) section-id))
           section-id))))




