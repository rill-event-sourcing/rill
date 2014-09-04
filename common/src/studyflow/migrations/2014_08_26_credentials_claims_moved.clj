(ns studyflow.migrations.2014-08-26-credentials-claims-moved
  (:require [rill.message :as message]))

(def typemap
  {:studyflow.school-administration.student.events/EmailAddressClaimed
   :studyflow.credentials.email-ownership.events/Claimed,

   :studyflow.school-administration.student.events/EmailAddressReleased
   :studyflow.credentials.email-ownership.events/Released,

   :studyflow.school-administration.student.events/EduRouteIdClaimed
   :studyflow.credentials.edu-route-id-ownership.events/Claimed,

   :studyflow.school-administration.student.events/EduRouteReleased
   :studyflow.credentials.edu-route-id-ownership.events/Released})

(defn move-credentials-claims
  [e]
  (update-in e [message/type] #(get typemap % %)))
