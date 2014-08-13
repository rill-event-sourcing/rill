(ns studyflow.learning.student-entry-quiz.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.material :as m]
            [studyflow.learning.entry-quiz-material :as eqm]
            [schema.core :as s]))

(defn student-entry-quiz-id
  [{:keys [entry-quiz-id student-id]}]
  (str "student-entry-quiz:" entry-quiz-id ":" student-id))

(defevent Created
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  student-entry-quiz-id)

(defevent QuestionAssigned
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  :question-id m/Id
  student-entry-quiz-id)

(defevent QuestionAnswered
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  student-entry-quiz-id)

(defevent QuizPassed
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  student-entry-quiz-id)

(defevent QuizFailed
  :entry-quiz-id eqm/EntryQuizId
  :student-id m/Id
  student-entry-quiz-id)
