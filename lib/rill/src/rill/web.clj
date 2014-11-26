(ns rill.web
  (:require [clojure.tools.logging :as log]
            [rill.handler :as rill-handler]
            [rill.message :as message]))

(defn command-result-to-ring-response [command [status events new-version triggered]]
  (case status
    :rejected
    {:status 422 :body {:status :command-rejected}} ; HTTP 422 Unprocessable Entity

    :conflict
    {:status 409 :body {:status :command-conflict}} ; HTTP 409 Conflict

    :out-of-date
    {:status 412 :body {:status :command-out-of-date}} ; HTTP 412 Precondition Failed

    :ok
    {:status 200 :body {:status :command-accepted
                        :events events
                        :aggregate-version new-version
                        :triggered triggered
                        :aggregate-id (message/primary-aggregate-id command)}}

    ;; else
    {:status 500 :body {:status :internal-error}}))

(defn wrap-command-handler
  "Given a ring handler that returns a command (or nil), execute the
  command with the given event store and return status 500 or 200"
  [ring-handler event-store]
  (fn [request]
    (when-let [command (ring-handler request)]
      (log/debug ["Executing command" command])
      (command-result-to-ring-response command (rill-handler/try-command event-store command)))))
