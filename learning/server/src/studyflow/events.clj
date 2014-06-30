(ns studyflow.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defevent CoursePublished
  :course-id m/Id
  :material m/CourseMaterial)

(defevent CourseUpdated
  :course-id m/Id
  :material m/CourseMaterial)

(defevent CourseDeleted
  :course-id m/Id)
