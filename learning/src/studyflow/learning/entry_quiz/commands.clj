(ns studyflow.learning.entry-quiz.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.entry-quiz-material :as material]))

(defcommand Publish!
  :entry-quiz-id material/EntryQuizId
  :material material/EntryQuizMaterial)

(defcommand Update!
  :entry-quiz-id material/EntryQuizId
  :material material/EntryQuizMaterial)
