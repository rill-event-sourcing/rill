(ns studyflow.learning.section-test.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.material :as m]
            [studyflow.learning.course-material :as cm]
            [schema.core :as s]))

(defn section-test-id
  [{:keys [section-id student-id]}]
  (str "section-test:" section-id ":" student-id))

(defevent Created
  :section-id m/Id
  :student-id m/Id
  :course-id cm/CourseId
  section-test-id)

(defevent QuestionAssigned
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  section-test-id)

(defevent AnswerRevealed
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  :answer s/Str
  section-test-id)

(defevent QuestionAnsweredCorrectly
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  section-test-id)

(defevent QuestionAnsweredIncorrectly
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  section-test-id)

(defevent Finished
  :section-id m/Id
  :student-id m/Id
  section-test-id)

(defevent StreakCompleted
  :section-id m/Id
  :student-id m/Id
  section-test-id)
