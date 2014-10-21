(ns studyflow.login.edu-route-student-test
  (:require
   [studyflow.login.edu-route-student.events :as events]
   [studyflow.login.edu-route-student :as student]
   [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
   [rill.uuid :refer [new-id]]
   [rill.message :as message]
   [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
   [clojure.test :refer [deftest testing is]]))

(def edu-route-id "123456")
(def full-name "Joost")
(def brin-code "abc")

(deftest test-commands
  (is (command-result= [:ok [(events/registered edu-route-id full-name brin-code)]]
                       (execute (student/register! edu-route-id full-name brin-code)
                                [])))
  (is (command-result= [:rejected]
                       (execute (student/register! edu-route-id full-name brin-code)
                                [(events/registered edu-route-id "Other name" "foo")]))))
