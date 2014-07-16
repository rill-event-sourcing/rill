(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [studyflow.schema-tools :as schema-tools]))

(def RichText s/Str)
(def PlainText s/Str)
(def Id s/Uuid)
(def FieldName s/Str)

(def Choice
  {:value s/Str
   :correct s/Bool})

(def MultipleChoiceInputField
  {:name FieldName
   :choices #{Choice}})

(def Answer
  {:value s/Str})

(def LineInputField
  {:name FieldName
   :pre s/Str
   :post s/Str
   :width s/Int
   :correct-answers #{Answer}})

(def CourseQuestion
  {:id Id
   :text RichText
   :worked-out-answer RichText
   :line-input-fields #{LineInputField}
   :multiple-choice-input-fields #{MultipleChoiceInputField}})

(def SectionQuestion
  {:id Id
   :text RichText
   :worked-out-answer RichText
   :line-input-fields #{LineInputField}
   :multiple-choice-input-fields #{MultipleChoiceInputField}})

(def SubSection
  {:id Id
   :title PlainText
   :text RichText})

(def Section
  {:id Id
   :title PlainText
   :subsections [SubSection]
   :questions #{SectionQuestion}})

(def Chapter
  {:id Id
   :title PlainText
   :sections [Section]})

(def CourseMaterial
  {:id Id
   :name PlainText
   :chapters [Chapter]
   :course-questions #{CourseQuestion}})

(def parse-course-material*
  (coerce/coercer CourseMaterial schema-tools/schema-coercion-matcher))

(def parse-course-material
  (schema-tools/strict-coercer parse-course-material*))



