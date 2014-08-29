(ns studyflow.school-administration.teacher.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(def EmailPasswordCredentials
  {:email s/Str
   :encryped-password s/Str})

(defevent Created
  :teacher-id s/Uuid
  :department-id s/Uuid
  :full-name s/Str)

(defevent CredentialsAdded
  :teacher-id s/Uuid
  :credentials EmailPasswordCredentials)

(defevent CredentialsChanged
  :teacher-id s/Uuid
  :credentials EmailPasswordCredentials)

(defevent EmailAddressClaimed
  :owner-id s/Uuid
  :email s/Str)

(defevent EmailAddressReleased
  :owner-id s/Uuid
  :email s/Str)

(defevent NameChanged
  :teacher-id s/Uuid
  :full-name s/Str)

(defevent EmailChanged
  :teacher-id s/Uuid
  :email s/Str)

(defevent DepartmentChanged
  :teacher-id s/Uuid
  :department-id s/Uuid)

(defevent ClassAssigned
  :teacher-id s/Uuid
  :department-id s/Uuid
  :class-name s/Str)

(defevent ClassUnassigned
  :teacher-id s/Uuid
  :department-id s/Uuid
  :class-name s/Str)
