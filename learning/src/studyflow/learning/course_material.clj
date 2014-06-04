(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [schema.macros :as sm]
            [schema.core :as s]))

(def RichText s/Str)

(def PlainText s/Str)

(sm/defrecord SubSection
    [id :- s/Uuid
     title :- PlainText
     text :- RichText])

(def Hint RichText)

(def Answer PlainText)

(sm/defrecord SectionEndTestQuestion
    [id :- s/Uuid
     text :- RichText
     hints :- #{Hint}
     choices :- {Answer RichText}
     correct-answers :- #{Answer}
     incorrect-answers :- #{Answer}])

(def ContentLevel s/Int)

(sm/defrecord Section
    [id :- s/Uuid
     title :- PlainText
     end-test-questions :- #{SectionEndTestQuestion}
     subsections-for-level :- {ContentLevel [SubSection]}])

(sm/defrecord Chapter
    [id :- s/Uuid
     title :- PlainText])

(sm/defrecord CourseMaterial
    [id :- s/Uuid
     name :- PlainText
     chapters :- [Chapter]])


