(ns studyflow.cli.validate-course-material-json-test
  (:require [studyflow.cli.validate-course-material-json :refer [validate-course-material]]
            [clojure.test :refer [deftest is]]))

(deftest test-validate-course-material
  (is (not (validate-course-material (slurp "test/studyflow/material.json")))))

