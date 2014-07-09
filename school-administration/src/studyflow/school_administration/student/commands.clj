(ns studyflow.school-administration.student.commands
  (:require [rill.message :refer [defcommand]]
            [schema.core :as s]))

(defcommand CreateStudent!
  :student-id s/Uuid
  :full-id s/Str)

