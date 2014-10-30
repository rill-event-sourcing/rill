(ns studyflow.learning.chapter-quiz.events
  (:require [rill.message :refer [defevent process-manager-id]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defn chapter-quiz-id
  [{:keys [chapter-id student-id]}]
  (str "chapter-quiz:" chapter-id ":" student-id))

(defevent Started
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :fast-route? s/Bool
  chapter-quiz-id)

(defevent QuestionAnsweredCorrectly
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  chapter-quiz-id)

(defevent QuestionAnsweredIncorrectly
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  chapter-quiz-id)

(defevent QuestionAssigned
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :question-set-id m/Id
  :question-id m/Id
  chapter-quiz-id)

(defevent Failed
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent Passed
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent Stopped
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent Locked
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent UnLocked
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defevent SectionFinished
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :section-id m/Id
  :all-sections #{m/Id}
  chapter-quiz-id)

(defmethod process-manager-id ::SectionFinished
  [event]
  (chapter-quiz-id event))

