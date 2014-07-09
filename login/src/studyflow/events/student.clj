(ns studyflow.events.student
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent CredentialsAdded
  :student-id s/Uuid
  :email s/Str
  :encrypted-password s/Str)

(defevent CredentialsChanged
  :student-id s/Uuid
  :email s/Str
  :encrypted-password s/Str)
