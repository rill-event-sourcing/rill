(ns studyflow.learning.course-material-test
  (:require [studyflow.learning.course-material :as material]
            [clojure.test :refer [is deftest testing]]
            [rill.uuid :refer [new-id]]
            [cheshire.core :as json]))

(defn read-example-json
  []
  (json/parse-string (slurp "../../material.json") true))

(deftest parsing-example-json
  (is (= (:name (material/parse-course-material (read-example-json)))
         "Math")))

