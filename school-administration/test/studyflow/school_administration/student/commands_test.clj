(ns studyflow.school-administration.student.commands-test
  (:require
   [studyflow.school-administration.student.events :as events]
   [studyflow.school-administration.student.commands :as commands]
   [studyflow.school-administration.student :as student]
   [rill.temp-store :refer [with-temp-store execute messages= message= command-result=]]
   [rill.uuid :refer [new-id]]
   [rill.message :as message]
   [rill.aggregate :refer [handle-event handle-command load-aggregate update-aggregate]]
   [clojure.test :refer [deftest testing is]]))

(deftest test-commands
  (testing "create students"
    (let [student-id (new-id)]
      (is (command-result= [:ok [(events/created student-id "Joost")]]
                           (execute (commands/create! student-id "Joost")
                                    []))))))

