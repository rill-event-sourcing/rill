(ns studyflow.command-tools
  (:require [rill.handler :refer [try-command]]
            [rill.aggregate :refer [handle-command update-aggregate]]
            [clojure.tools.logging :as log]))

(defn with-claim [event-store claim main revert-claim]
  (let [[claim-status :as claim-result] (try-command event-store claim)]
    (if (= :ok claim-status)
      (let [[main-status :as main-result] (try-command event-store main)]
        (when-not (= :ok main-status)
          (try-command event-store revert-claim))
        main-result)
      claim-result)))


;; for this to work more generally, we should deal with additional aggregate-ids
;; for each command.
;; in any case, this will only work for commands that apply to the same aggregate

(defn apply-command
  [{aggregate-status ::status :as aggregate} command]
  (if (or (nil? aggregate-status)
          (= :ok aggregate-status))
    (let [[status events] (handle-command aggregate command)]
      (if (= :ok status)
        (do (log/info [aggregate events])
            (-> aggregate
                (update-aggregate events)
                (update-in [::uncommitted-events] (fnil into []) events)
                (assoc ::status :ok)))
        {::status status
         ::rest events}))
    aggregate))

(defn combine-commands
  [aggregate & commands]
  (try
    (let [a (reduce apply-command aggregate commands)]
      (if (= :ok (::status a))
        [:ok (::uncommitted-events a)]
        [(::status a) (::rest a)]))
    (catch Throwable t
      [:rejected {:exception t}])))
