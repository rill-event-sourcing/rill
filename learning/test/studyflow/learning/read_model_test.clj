(ns studyflow.learning.read-model-test
  (:require [clojure.test :refer [is deftest testing]]
            [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.read-model.event-handler :refer [init-model]]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.course.fixture :refer [course-published-event course-id course-edn]]
            [rill.uuid :refer [new-id]]
            [rill.temp-store :refer [given]]))

(def section-id (:id (first (:sections (first (:chapters course-edn))))))
(def student-id (new-id))

(deftest read-model-test
  (testing "Chapters and learning steps can be requested as a tree"
    (let [model (init-model [course-published-event
                             (section-test/finished section-id student-id)])]
      (is (= (get-in (model/course-tree model course-id nil)
                     [:chapters 0 :sections 1 :title])
             "Position of the 0"))

      (is (= (set (keys (get-in (model/course-tree model course-id student-id)
                                [:chapters 0 :sections 1])))
             #{:title :id :status})
          "no subsection data in the course tree")

      (testing "section status is included for students"
        (is (= (get-in (model/course-tree model course-id student-id)
                       [:chapters 0 :sections 0 :status])
               :finished)
            "subsection status")

        (is (= (get-in (model/course-tree model course-id student-id)
                       [:chapters 0 :sections 1 :status])
               nil)
            "subsection status"))

      (assert section-id)
      (is (= (:id (model/get-section (model/get-course model course-id) section-id))
             section-id)))))
