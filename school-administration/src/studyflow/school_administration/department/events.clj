(ns studyflow.school-administration.department.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defevent Created
  :department-id s/Uuid
  :school-id s/Uuid
  :name s/Str)

(defevent NameChanged
  :department-id s/Uuid
  :name s/Str)

(defevent SalesDataChanged
  :department-id s/Uuid
  :licenses-sold s/Int
  :status s/Str)
