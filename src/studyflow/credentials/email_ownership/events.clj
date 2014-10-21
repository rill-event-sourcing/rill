(ns studyflow.credentials.email-ownership.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(defn make-aggregate-id
  "Make a per-email unique id for events and commands"
  [{:keys [email]}]
  (str "email-ownership-" email))

(defevent Claimed
  :owner-id s/Uuid
  :email s/Str
  make-aggregate-id)

(defevent Released
  :owner-id s/Uuid
  :email s/Str
  make-aggregate-id)
