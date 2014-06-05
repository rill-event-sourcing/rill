(ns studyflow.learning.t-course-material
  (:require [studyflow.learning.course-material :as material]
            [midje.sweet :refer :all]
            [rill.uuid :refer [new-id]]
            [cheshire.core :as json]))

(defn read-example-json
  []
  (json/parse-string (slurp "../../material.json")))

(facts "We can parse the example json"
       (:name (material/parse-course-material (read-example-json))) => "Math")

