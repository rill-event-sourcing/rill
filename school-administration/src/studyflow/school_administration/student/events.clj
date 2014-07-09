(ns studyflow.school-administration.student.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent Created
  :student-id s/Uuid
  :full-name s/Str)

(defevent CredentialsAdded
  :student-id s/Uuid
  :email s/Str
  :encrypted-password s/Str)

(defevent NameChanged
  :student-id s/Uuid
  :full-name s/Str)

(defevent EmailChanged
  :student-id s/Uuid
  :email s/Str)

(defevent PasswordChanged
  :student-id s/Uuid
  :encrypted-password s/Str)
