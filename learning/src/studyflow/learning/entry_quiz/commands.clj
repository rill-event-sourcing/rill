(ns studyflow.learning.entry-quiz.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.entry-quiz-material :as material]))

(defcommand Publish!
  :course-id material/EntryQuizId
  :material material/EntryQuizMaterial)

(defcommand Update!
  :course-id material/EntryQuizId
  :material material/EntryQuizMaterial)
