(ns studyflow.school-administration.student.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(def EmailPasswordCredentials
  {:email s/Str
   :encryped-password s/Str})

(defevent Created
  :student-id s/Uuid
  :full-name s/Str)

(defevent CredentialsAdded
  :student-id s/Uuid
  :credentials EmailPasswordCredentials)

(defevent CredentialsChanged
  :student-id s/Uuid
  :credentials EmailPasswordCredentials)

(defn edu-route-claim-id
  [{:keys [edu-route-id]}]
  (str "edu-route-id-ownership-" edu-route-id))

(defevent EduRouteIdClaimed
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defevent EduRouteIdReleased
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defevent EduRouteCredentialsAdded
  :student-id s/Uuid
  :edu-route-id s/Str)

(defevent EmailAddressClaimed
  :owner-id s/Uuid
  :email s/Str)

(defevent EmailAddressReleased
  :owner-id s/Uuid
  :email s/Str)

(defevent NameChanged
  :student-id s/Uuid
  :full-name s/Str)

(defevent EmailChanged
  :student-id s/Uuid
  :email s/Str)

(defevent DepartmentChanged
  :student-id s/Uuid
  :department-id s/Uuid)

(defevent ClassAssigned
  :student-id s/Uuid
  :department-id s/Uuid
  :class-name s/Str)

(defevent Imported
  :student-id s/Uuid
  :full-name s/Str
  :department-id s/Uuid
  :class-name s/Str
  :credentials EmailPasswordCredentials)
