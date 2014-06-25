(ns studyflow.learning.course-material-test
  (:require [studyflow.learning.course-material :as material]
            [clojure.test :refer [is deftest testing]]
            [rill.uuid :refer [new-id]]
            [cheshire.core :as json]
            [studyflow.json-tools :refer [key-from-json]]))

(defn read-example-json
  []
  (json/parse-string (slurp "test/studyflow/material.json") key-from-json))

(deftest parsing-test
  (testing "parsing example json"
    (is (= (:name (material/parse-course-material (read-example-json)))
           "Math")))

  (testing "throws exceptions when not valid"
    (is (thrown? RuntimeException (material/parse-course-material {:id "invalid" :name "Math"})))))
