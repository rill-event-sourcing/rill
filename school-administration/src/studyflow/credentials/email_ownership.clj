(ns studyflow.credentials.email-ownership
  (:require [studyflow.credentials.email-ownership.events :as events :refer [claimed released make-aggregate-id]]
            [rill.message :refer [defcommand]]
            [rill.aggregate :refer [handle-event handle-command]]
            [schema.core :as s]))

(def error-messages {:can-not-be-blank "can't be blank"
                     :already-claimed "already claimed"
                     :ownership-disputed "ownership disputed"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; claiming an email adress

(defcommand Claim!
  :owner-id s/Uuid
  :email s/Str
  make-aggregate-id)

(defmethod handle-command ::Claim!
  [{:keys [current-owner-id]} {:keys [owner-id email]}]
  (if (or (nil? current-owner-id)
          (= owner-id current-owner-id))
    [:ok [(claimed owner-id email)]]
    [:rejected {:email [(:already-claimed error-messages)]}]))

(defmethod handle-event ::events/Claimed
  [claim {:keys [owner-id]}]
  (assoc claim :current-owner-id owner-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; releasing an email adress

(defcommand Release!
  :owner-id s/Uuid
  :email s/Str
  make-aggregate-id)

(defmethod handle-command ::Release!
  [{:keys [current-owner-id]} {:keys [owner-id email]}]
  (if (= current-owner-id owner-id)
    [:ok [(released owner-id email)]]
    [:rejected {:email [(:ownership-disputed error-messages)]}]))

(defmethod handle-event ::events/Released
  [claim event]
  (dissoc claim :current-owner-id))
