(ns studyflow.learning.tracking.commands
  (:require [rill.message :refer [defcommand]]
            [schema.core :as s]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.tracking.events :refer [tracking-id]]))

(defcommand Navigate!
  :student-id m/Id
  :tracking-location {s/Keyword #{s/Keyword m/Id}}
  tracking-id)
