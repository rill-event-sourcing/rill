(ns studyflow.credentials.edu-route-id-ownership.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defn edu-route-claim-id
  [{:keys [edu-route-id]}]
  (str "edu-route-id-ownership-" edu-route-id))

(defevent Claimed
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defevent Released
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)


