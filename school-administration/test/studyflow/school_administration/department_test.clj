(ns studyflow.school-administration.department-test
  (:require
   [studyflow.school-administration.department.events :as events]
   [studyflow.school-administration.school.events :as school]
   [studyflow.school-administration.department :as department]
   [rill.temp-store :refer [execute command-result=]]
   [rill.uuid :refer [new-id]]
   [clojure.test :refer [deftest testing is]]))

(def school-id (new-id))
(def department-id (new-id))

(deftest test-commands
  (testing "create department"
    (is (command-result= [:ok [(events/created department-id school-id "DEPARTMENT")]]
                         (execute (department/create! department-id school-id "DEPARTMENT" )
                                  [(school/created school-id "school" "123T")]))))

  (testing "changing name"
    (testing "with an empty name"
      (is (= :rejected
             (first (execute (department/change-name! department-id 0 "")
                             [(school/created school-id "school" "123T")
                              (events/created department-id school-id "DEPARTMENT")])))))

    (testing "with a proper name"
      (is (command-result= [:ok [(events/name-changed department-id "DEP")]]
                           (execute (department/change-name! department-id 0 "DEP")
                                    [(school/created school-id "school" "123T")
                                     (events/created department-id school-id "DEPARTMENT")])))))

  (testing "changing sales data"
    (is (command-result= [:ok [(events/sales-data-changed department-id 123 "pilot")]]
                         (execute (department/change-sales-data! department-id 0 123 "pilot")
                                  [(school/created school-id "school" "123T")
                                   (events/created department-id school-id "DEPARTMENT")])))))
