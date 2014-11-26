(ns studyflow.learning.section-test.events
  (:require [rill.message :refer [defevent observers]]
            [studyflow.learning.chapter-quiz.notifications :as chapter-quiz]
            [studyflow.learning.section-bank.notifications :as section-bank]
            [studyflow.learning.chapter-quiz.events :refer [chapter-quiz-id]]
            [studyflow.learning.section-bank.events :refer [section-bank-id]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defn section-test-id
  [{:keys [section-id student-id]}]
  (str "section-test:" section-id ":" student-id))

(defevent Created
  :section-id m/Id
  :student-id m/Id
  :course-id m/Id
  section-test-id)

(defevent QuestionAssigned
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  :question-total s/Int
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

(defmethod observers ::QuestionAnsweredCorrectly
  [event]
  [[(section-bank-id event) section-bank/notify]])

(defevent QuestionAnsweredIncorrectly
  :section-id m/Id
  :student-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  section-test-id)

(defevent Finished
  :section-id m/Id
  :student-id m/Id
  :chapter-id m/Id
  :course-id m/Id
  section-test-id)

(defmethod observers ::Finished
  [event]
  [[(chapter-quiz-id event) chapter-quiz/notify]])

(defevent StreakCompleted
  :section-id m/Id
  :student-id m/Id
  section-test-id)

(defevent Stuck
  :section-id m/Id
  :student-id m/Id
  section-test-id)

(defevent Unstuck
  :section-id m/Id
  :student-id m/Id
  section-test-id)

(defevent ModalDismissed
  :section-id m/Id
  :student-id m/Id
  section-test-id)
