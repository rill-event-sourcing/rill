(ns studyflow.system.components.event-channel
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [rill.event-channel :refer [event-channel]]
            [clojure.tools.logging :refer [info debug spy]]
            [clojure.core.async :refer [close!]]))

(defrecord EventChannelComponent [event-store]
  Lifecycle
  (start [component]
    (info "Starting event-channel")
    (assoc component
      :channel (event-channel (:store event-store) "%24all" -1 0)))
  (stop [component]
    (info "Stopping event-channel")
    (when-let [c (:channel component)]
      (close! c))
    component))

(defn event-channel-component []
  (map->EventChannelComponent {}))
