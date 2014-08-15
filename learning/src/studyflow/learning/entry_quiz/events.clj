(ns studyflow.learning.entry-quiz.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defn entry-quiz-id
  [{:keys [course-id student-id]}]
  (str "entry-quiz:" course-id ":" student-id))

(defevent NagScreenDismissed
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defevent Started
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defevent InstructionsRead
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defevent QuestionAnsweredCorrectly
  :course-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  entry-quiz-id)

(defevent QuestionAnsweredIncorrectly
  :course-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  entry-quiz-id)

(defevent Failed
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defevent Passed
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)


