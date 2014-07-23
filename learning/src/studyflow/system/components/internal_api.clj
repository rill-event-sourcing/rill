(ns studyflow.system.components.internal-api
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.web.internal-api :as internal-api]))

(defrecord InternalApiComponent [event-store]
  Lifecycle
  (start [component]
    (log/info "Starting internal-api handler")
    (assoc component :handler (internal-api/make-request-handler (:store event-store))))
  (stop [component]
    (log/info "Stopping internal-api handler")
    component))

(defn internal-api-component []
  (map->InternalApiComponent {}))
