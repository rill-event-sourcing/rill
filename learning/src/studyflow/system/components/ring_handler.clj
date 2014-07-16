(ns studyflow.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.web :as web]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord RingHandlerComponent [event-store read-model]
  Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler (web/make-request-handler (:store event-store) (:read-model read-model))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn ring-handler-component []
  (map->RingHandlerComponent {}))
