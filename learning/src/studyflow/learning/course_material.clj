(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [studyflow.learning.material :as m]
            [studyflow.schema-tools :as schema-tools]))

(def CourseId m/Id)

(def Tool
  (s/enum "pen_and_paper" "calculator"))

(def SectionQuestion
  {:id m/Id
   :text m/RichText
   :tools #{Tool}
   :line-input-fields [m/LineInputField]
   :multiple-choice-input-fields [m/MultipleChoiceInputField]
   (s/optional-key :worked-out-answer) m/RichText})

(def SubSection
  {:id m/Id
   :title m/PlainText
   :text m/RichText})

(def Section
  {:id m/Id
   :title m/PlainText
   :subsections [SubSection]
   :line-input-fields #{m/LineInputField}
   :questions (s/both #{SectionQuestion}
                      (s/pred (fn [s] (seq s)) 'not-empty))})

(def Chapter
  {:id m/Id
   :title m/PlainText
   :sections [Section]})

(def CourseMaterial
  {:id CourseId
   :name m/PlainText
   :chapters [Chapter]})

(def parse-course-material*
  (coerce/coercer CourseMaterial schema-tools/schema-coercion-matcher))

(def parse-course-material
  (comp
   m/transform-question-text-to-tree
   (schema-tools/strict-coercer parse-course-material*)))



