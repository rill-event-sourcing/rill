(ns studyflow.components.memory-event-store
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [rill.event-store.memory :refer [memory-store]]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord MemoryEventStoreComponent []
  Lifecycle
  (start [component]
    (info "Starting in-memory event-store")
    (assoc component :store (memory-store)))
  (stop [component]
    (info "Stopping in-memory event-store")
    component))

(defn memory-event-store-component []
  (->MemoryEventStoreComponent))
