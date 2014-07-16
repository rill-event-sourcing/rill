(ns studyflow.learning.section-test.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defcommand CheckAnswer!
  :section-test-id m/Id
  :section-id m/Id
  :course-id m/Id
  :question-id m/Id
  :inputs {m/FieldName s/Str})

(def SectionTestId (s/pred (fn [str]
                             (and str
                                  (.contains str "student-id")
                                  (.contains str "section-id"))) 'is-section-test-id))
(defcommand Init!
  :section-test-id SectionTestId
  :section-id m/Id
  :course-id m/Id)

(defcommand NextQuestion!
  :section-test-id m/Id
  :section-id m/Id
  :course-id m/Id)


