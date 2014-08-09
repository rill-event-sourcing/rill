(ns studyflow.learning.course.events
  (:require [rill.message :refer [defevent]]
            [studyflow.learning.course-material :as m]
            [schema.core :as s]))

(defevent Published
  :course-id m/CourseId
  :material m/CourseMaterial)

(defevent Updated
  :course-id m/CourseId
  :material m/CourseMaterial)

(defevent Deleted
  :course-id m/CourseId)

