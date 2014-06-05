(ns studyflow.learning.t-read-model
  (:require [midje.sweet :refer :all]
            [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.read-model.event-handler :as handler]
            [studyflow.events :as events]
            [rill.uuid :refer [new-id]]))

(defn read-example-json
  []
  (json/parse-string (slurp "../../material.json")))

(def editor-id 333)

(facts "We can parse the example json"
       (:name (material/parse-course-material (read-example-json))) => "Math")

(facts "Published chapters and learning steps can be requested by ID"
       (:title (model/get-chapter initial-model 1)) => "Positional notation"
       (:title (model/get-learning-step initial-model 2)) => "Position and 0")

(facts "Chapters and learning steps can be requested as a tree"
       (model/course-tree initial-model 6) =>
       {:id 6
        :title "Math"
        :chapters [{:id 1
                    :title "Positional notation"
                    :learning-steps [{:id 2
                                      :title "Position and 0"}]}
                   {:id 3
                    :title "Another chapter"
                    :learning-steps []}]})
