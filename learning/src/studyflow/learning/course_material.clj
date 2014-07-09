(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [studyflow.schema-tools :as schema-tools]))

(def RichText s/Str)
(def PlainText s/Str)
(def Id s/Uuid)
(def TextLevel s/Int)

(def SubSection
  {:id Id
   :title PlainText
   :text RichText})

(def ContentLevel s/Int)

(def FieldName s/Str)

(def InputField
  {:name FieldName
   :correct-answers #{s/Str}})

(def SectionQuestion
  {:id Id
   :text RichText
   :input-fields #{InputField}})

(def Section
  {:id Id
   :title PlainText
   :subsections-by-level {:1-star [SubSection]
                          :2-star [SubSection]
                          :3-star [SubSection]}
   :questions #{SectionQuestion}})

(def Chapter
  {:id Id
   :title PlainText
   :sections [Section]})

(def CourseMaterial
  {:id Id
   :name PlainText
   :chapters [Chapter]})

(def parse-course-material*
  (coerce/coercer CourseMaterial schema-tools/schema-coercion-matcher))

(def parse-course-material
  (schema-tools/strict-coercer parse-course-material*))



