(ns studyflow.learning.chapter-quiz.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defn chapter-quiz-id
  [{:keys [chapter-id student-id]}]
  (str "chapter-quiz:" chapter-id ":" student-id))

(defevent Started
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent QuestionAnsweredCorrectly
  :chapter-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  chapter-quiz-id)

(defevent QuestionAnsweredIncorrectly
  :chapter-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  chapter-quiz-id)

(defevent Failed
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent Passed
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent Stopped
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

