(ns studyflow.school-administration.student.commands
  (:require [rill.message :refer [defcommand]]
            [schema.core :as s]))

(defcommand Create!
  :student-id s/Uuid
  :full-name s/Str)

