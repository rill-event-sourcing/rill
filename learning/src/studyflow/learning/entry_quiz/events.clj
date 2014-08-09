(ns studyflow.learning.entry-quiz.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.entry-quiz-material :as m]
            [schema.core :as s]))

(defevent Published
  :entry-quiz-id m/EntryQuizId
  :material m/EntryQuizMaterial)

(defevent Updated
  :entry-quiz-id m/EntryQuizId
  :material m/EntryQuizMaterial)
