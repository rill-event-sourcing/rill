(ns studyflow.learning.t-course-material
  (:require [studyflow.learning.course-material :as material]
            [rill.uuid :refer [new-id]]))

(defn test-parser
  []
  (material/parse-course-material  {:id (new-id) :name "foo" :chapters
                                    [{:id (new-id) :title "bla"}]}))

