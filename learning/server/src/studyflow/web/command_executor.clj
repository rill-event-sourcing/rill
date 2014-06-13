(ns studyflow.web.command-executor
  (:require [clojure.tools.logging :as log]
            [rill.handler :as es-dispatcher]))

(defn wrap-command-executor
  "Given a set of ring handler that returns a command (or nil), execute
  the command with the given event store and return status 500 or 200"
  [ring-handler event-store]
  (fn [request]
    (when-let [command (ring-handler request)]
      (log/info ["Executing command" (class command)])
      (if (= :es-dispatcher/error (es-dispatcher/try-command event-store command))
        {:status 500 :body {:status :command-rejected}}
        {:status 200 :body {:status :command-accepted}}))))
