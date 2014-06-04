(ns studyflow.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material]
            [schema.core :as s])
  (:import (studyflow.learning.course_material CourseMaterial Chapter Section SubSection)))

(defevent CoursePublished
  [course-id :- s/Uuid
   publisher-id :- s/Uuid
   material :- CourseMaterial])

(defevent CourseUpdated
  [course-id :- s/Uuid
   publisher-id :- s/Uuid
   material :- CourseMaterial])

(defevent CourseDeleted
  [course-id :- s/Uuid
   publisher-id :- s/Uuid])



