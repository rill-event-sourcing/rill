(ns studyflow.learning.section-test.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.material :as m]
            [studyflow.learning.course-material :as cm]
            [studyflow.learning.section-test.events :refer [section-test-id]]
            [schema.core :as s]))

(defcommand CheckAnswer!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id cm/CourseId
  :question-id m/Id
  :inputs {m/FieldName s/Str}
  section-test-id)

(defcommand RevealAnswer!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id cm/CourseId
  :question-id m/Id
  section-test-id)

(defcommand Init!
  :section-id m/Id
  :student-id m/Id
  :course-id cm/CourseId
  section-test-id)

(defcommand NextQuestion!
  :section-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :course-id cm/CourseId
  section-test-id)
