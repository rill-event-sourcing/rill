(ns studyflow.command-tools
  (:require [clojure.tools.logging :as log]
            [rill.handler :refer [try-command]]))

(defn with-claim [event-store claim main revert-claim]
  (let [[claim-status :as claim-result] (try-command event-store claim)]
    (if (= :ok claim-status)
      (let [[main-status :as main-result]
            (try (try-command event-store main)
                 (catch Exception e
                   (log/error e)
                   (try-command event-store revert-claim)
                   (throw e)))]
        (when-not (= :ok main-status)
          (try-command event-store revert-claim))
        main-result)
      claim-result)))
