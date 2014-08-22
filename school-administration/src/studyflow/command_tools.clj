(ns studyflow.command-tools
  (:require [rill.handler :refer [try-command]]))

(defn with-claim [event-store claim main revert-claim]
  (let [[claim-status :as claim-result] (try-command event-store claim)]
    (if (= :ok claim-status)
      (let [[main-status :as main-result] (try-command event-store main)] ; TODO catch exception and revert claim
        (when-not (= :ok main-status)
          (try-command event-store revert-claim))
        main-result)
      claim-result)))
