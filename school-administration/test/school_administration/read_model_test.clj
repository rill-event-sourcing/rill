(ns studyflow.school-administration.read-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [studyflow.school-administration.read-model :as m]
            [rill.uuid :refer [new-id]]
            [studyflow.school-administration.student.events :as student]
            [studyflow.school-administration.read-model.event-handler :refer [load-model]]))

(def students
  [{:id (new-id)
    :full-name "Joost Diepenmaat"}
   {:id (new-id)
    :full-name "Steven Thonus"}])

(deftest test-list-students
  (is (empty? (m/list-students nil)))

  (is (= students
         (m/list-students (load-model [(student/created (:id (first students))
                                                        (:full-name (first students)))
                                       (student/created (:id (second students))
                                                        (:full-name (second students)))])))))
