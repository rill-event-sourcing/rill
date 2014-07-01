(ns studyflow.learning.course.commands
  (:require [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as material]))

(defcommand Publish!
  :course-id material/Id
  :material material/CourseMaterial)

(defcommand Update!
  :course-id material/Id
  :material material/CourseMaterial)

(defcommand Delete!
  :course-id material/Id)

