(ns studyflow.learning.section-test.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defevent Created
  :section-test-id m/Id
  :course-id m/Id
  :section-id m/Id)

(defevent QuestionAssigned
  :section-test-id m/Id
  :course-id m/Id
  :question-id m/Id)

(defevent QuestionAnsweredCorrectly
  :section-test-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str})

(defevent QuestionAnsweredIncorrectly
  :section-test-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str})

