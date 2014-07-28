(ns studyflow.school-administration.school.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent Created
  :school-id s/Uuid
  :name s/Str
  :brin s/Str)

(defevent NameChanged
  :school-id s/Uuid
  :name s/Str)

(defevent BrinClaimed
  :owner-id s/Uuid
  :brin s/Str)

(defevent BrinReleased
  :owner-id s/Uuid
  :brin s/Str)
