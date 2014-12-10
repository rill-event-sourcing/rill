(ns studyflow.learning.validate-course-material-json-test
  (:require [studyflow.learning.validate-course-material-json :refer [validate-course-material]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]))

(deftest test-validate-course-material
  (is (not (validate-course-material (slurp (io/resource "dev/material.json"))))))

