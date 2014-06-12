(ns studyflow.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as material]
            [schema.core :as s]))

(defevent CoursePublished
  [course-id :- material/Id
   material :- material/CourseMaterial])

(defevent CourseUpdated
  [course-id :- material/Id
   material :- material/CourseMaterial])

(defevent CourseDeleted
  [course-id :- material/Id])




