(ns studyflow.learning.section-test.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.section-test.events :refer [section-test-id]]
            [schema.core :as s]))

(defcommand CheckAnswer!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  section-test-id)

(defcommand RevealAnswer!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id m/Id
  :question-id m/Id
  section-test-id)

(defcommand Init!
  :section-id m/Id
  :student-id m/Id
  :course-id m/Id
  section-test-id)

(defcommand NextQuestion!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id m/Id
  section-test-id)

(defcommand DismissModal!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id m/Id
  section-test-id)
