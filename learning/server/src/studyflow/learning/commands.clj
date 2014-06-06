(ns studyflow.learning.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as material]))

(defcommand PublishCourse!
  [course-id :- material/Id
   material :- material/CourseMaterial])

(defcommand UpdateCourse!
  [course-id :- material/Id
   material :- material/CourseMaterial])

(defcommand DeleteCourse!
  [course-id :- material/Id])


