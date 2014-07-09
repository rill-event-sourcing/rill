(ns studyflow.school-administration.student.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent StudentCreated
  :student-id s/Uuid
  :full-name s/Str)

(defevent StudentCredentialsAdded
  :student-id s/Uuid
  :email s/Str
  :encrypted-password s/Str)

(defevent StudentNameChanged
  :student-id s/Uuid
  :full-name s/Str)

(defevent StudentEmailChanged
  :student-id s/Uuid
  :email s/Str)

(defevent StudentPasswordChanged
  :student-id s/Uuid
  :encrypted-password s/Str )

