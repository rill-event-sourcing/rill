(ns studyflow.learning.course.fixture
  (:require [cheshire.core :as json]
            [studyflow.json-tools :refer [key-from-json]]
            [studyflow.learning.course-material :as material]))

(def course-json
  (json/parse-string (slurp "test/studyflow/material.json") key-from-json))

(def course-edn
  (material/parse-course-material course-json))

