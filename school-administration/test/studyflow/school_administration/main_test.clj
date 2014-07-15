(ns studyflow.school-administration.main-test
  (:require [studyflow.school-administration.main :as main]
            [studyflow.school-administration.student :as student]
            [rill.message :as message]
            [clojure.test :refer [is deftest testing]]
            [ring.mock.request :refer [request]]))

(deftest test-commands
  (testing "post create-student"
    (let [resp (main/commands (request :post "/create-student" {:full-name "Tinus"}))]
      (is (= ::student/Create! (message/type resp))))))
