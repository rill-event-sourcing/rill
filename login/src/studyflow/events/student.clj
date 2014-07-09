(ns studyflow.events.student
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent Created
  :student-id s/Uuid
  :full-name s/Str
  :email s/Str
  :encrypted-password s/Str)
