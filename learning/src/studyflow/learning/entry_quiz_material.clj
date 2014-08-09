(ns studyflow.learning.entry-quiz-material
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [studyflow.learning.material :as m]
            [studyflow.learning.course-material :as cm]
            [studyflow.schema-tools :as schema-tools]))

(def EntryQuizId m/Id)

(def Question
  {:id m/Id
   :text m/RichText
   :line-input-fields [m/LineInputField]
   :multiple-choice-input-fields [m/MultipleChoiceInputField]})

(def EntryQuizMaterial
  {:id EntryQuizId
   :course-id cm/CourseId
   :name m/PlainText
   :description m/RichText
   :questions [Question]})

(def parse-entry-quiz-material*
  (coerce/coercer EntryQuizMaterial schema-tools/schema-coercion-matcher))

(def parse-entry-quiz-material
  (comp
   m/transform-question-text-to-tree
   (schema-tools/strict-coercer parse-entry-quiz-material*)))
