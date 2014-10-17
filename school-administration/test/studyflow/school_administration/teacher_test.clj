(ns studyflow.school-administration.teacher-test
  (:require
   [studyflow.school-administration.teacher.events :as events]
   [studyflow.school-administration.department.events :as department]
   [studyflow.school-administration.school.events :as school]
   [studyflow.school-administration.teacher :as teacher]
   [rill.temp-store :refer [execute command-result=]]
   [rill.uuid :refer [new-id]]
   [clojure.test :refer [deftest testing is]]))

(def teacher-id (new-id))
(def school-id (new-id))
(def department-id (new-id))


(deftest test-commands
  (testing "create teachers"
    (is (command-result= [:ok [(events/created teacher-id department-id "Joost")]]
                         (execute (teacher/create! teacher-id department-id "Joost")
                                  [(school/created school-id "SCHOOL" "123T")
                                   (department/created department-id school-id "DEPT")]))))

  (testing "assigning classes"
    (is (command-result= [:ok [(events/class-assigned teacher-id department-id "CLASS_ONE")
                               (events/class-assigned teacher-id department-id "CLASS_TWO")]]
                         (execute (teacher/change-classes! teacher-id 1 #{"CLASS_ONE" "CLASS_TWO"})
                                  [(school/created school-id "SCHOOL" "123T")
                                   (department/created department-id school-id "DEPT")
                                   (events/created teacher-id department-id "Pietje Puk")
                                   (events/department-changed teacher-id department-id)])))

    (is (command-result= [:ok [(events/class-assigned teacher-id department-id "CLASS_THREE")
                               (events/class-unassigned teacher-id department-id "CLASS_ONE")]]
                         (execute (teacher/change-classes! teacher-id 3 #{"CLASS_TWO" "CLASS_THREE"})
                                  [(school/created school-id "SCHOOL" "123T")
                                   (department/created department-id school-id "DEPT")
                                   (events/created teacher-id department-id "Pietje Puk")
                                   (events/department-changed teacher-id department-id)
                                   (events/class-assigned teacher-id department-id "CLASS_ONE")
                                   (events/class-assigned teacher-id department-id "CLASS_TWO")])))))
