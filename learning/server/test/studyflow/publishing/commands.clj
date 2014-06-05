(ns studyflow.publishing.commands
  (:require [rill.message :refer [defcommand]]))

(defcommand UpdateCourseMaterial!
  [course-id author-id material])

