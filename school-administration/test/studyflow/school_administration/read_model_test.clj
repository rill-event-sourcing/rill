(ns studyflow.school-administration.read-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [studyflow.school-administration.read-model :as m]
            [rill.uuid :refer [new-id]]
            [rill.message :as message]
            [studyflow.school-administration.student.events :as student]
            [studyflow.school-administration.read-model.event-handler :refer [load-model]]))

(def students
  [{:id (new-id)
    :full-name "Joost Diepenmaat"
    :version 314
    :school nil
    :department nil}
   {:id (new-id)
    :full-name "Steven Thonus"
    :version 272
    :school nil
    :department nil}])

(deftest test-list-students
  (is (empty? (m/list-students nil)))

  (is (= students
         (m/list-students (load-model [(assoc (student/created (:id (first students))
                                                               (:full-name (first students)))
                                         message/number (:version (first students)))
                                       (assoc (student/created (:id (second students))
                                                               (:full-name (second students)))

                                         message/number (:version (second students)))])))))
