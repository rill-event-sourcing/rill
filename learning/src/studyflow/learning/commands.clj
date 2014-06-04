(ns studyflow.learning.commands
  (:require [rill.message :refer [defcommand]]))

(defcommand OpenLearningStep!
  [student-id learning-step-id work-id])

(defcommand AnswerQuestion!
  [work-id student-id learning-step-id question-id answer])

