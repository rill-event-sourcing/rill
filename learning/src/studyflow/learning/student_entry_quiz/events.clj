(ns studyflow.learning.student-entry-quiz.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.material :as m]
            [studyflow.learning.course-material :as cm]
            [schema.core :as s]))

(defn student-entry-quiz-id
  [{:keys [entry-quiz-id student-id]}]
  (str "student-entry-quiz:" entry-quiz-id ":" student-id))

(defevent Created
  :entry-quiz-id m/Id
  :student-id m/Id
  student-entry-quiz-id)
