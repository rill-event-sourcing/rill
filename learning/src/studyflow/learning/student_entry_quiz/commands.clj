(ns studyflow.learning.student-entry-quiz.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.material :as m]
            [studyflow.learning.entry-quiz-material :as eqm]
            [studyflow.learning.student-entry-quiz.events :refer [student-entry-quiz-id]]
            [schema.core :as s]))

(defcommand Init!
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  student-entry-quiz-id)
