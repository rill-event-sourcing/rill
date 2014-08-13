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

(defcommand SubmitAnswer!
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  :expected-version s/Int
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  student-entry-quiz-id)
