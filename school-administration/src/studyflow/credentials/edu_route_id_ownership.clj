(ns studyflow.credentials.edu-route-id-ownership
  (:require [studyflow.credentials.edu-route-id-ownership.events :as events :refer [claimed released edu-route-claim-id]]
            [rill.aggregate :refer [handle-command handle-event]]
            [rill.message :refer [defcommand]]
            [schema.core :as s]))

(def error-messages {:can-not-be-blank "can't be blank"
                     :already-claimed "already claimed"
                     :ownership-disputed "ownership disputed"})

(defcommand Claim!
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defmethod handle-command ::Claim!
  [{:keys [current-owner-id]} {:keys [owner-id edu-route-id]}]
  (if (or (nil? current-owner-id)
          (= owner-id current-owner-id))
    [:ok [(claimed edu-route-id owner-id)]]
    [:rejected {:edu-route-id [(:already-claimed error-messages)]}]))

(defcommand Release!
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defmethod handle-command ::Release!
  [{:keys [current-owner-id]} {:keys [owner-id edu-route-id]}]
  (if (= current-owner-id owner-id)
    [:ok [(released edu-route-id owner-id)]]
    [:rejected {:edu-route-id [(:ownership-disputed error-messages)]}]))

(defmethod handle-event ::events/Claimed
  [_ {:keys [owner-id edu-route-id]}]
  {:current-owner-id owner-id})

(defmethod handle-event ::events/Released
  [claim {:keys [owner-id edu-route-id]}]
  (dissoc claim :current-owner-id))
