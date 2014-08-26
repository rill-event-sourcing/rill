(ns studyflow.teaching.system.components.read-model
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.read-model :refer [empty-model]]
            [studyflow.teaching.read-model.event-listener :refer [listen!]]
            [clojure.core.async :refer [close! <!!]]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord ReadModelComponent [event-channel]
  Lifecycle
  (start [component]
    (info "Starting teaching read model")
    (let [read-model (atom empty-model)]
      (assoc component
        :read-model read-model
        :event-listener (listen! read-model (:channel event-channel)))))
  (stop [component]
    (info "Stopping teaching read model")
    ;; this has a dependency on the event-channel, which will start
    ;; before as and therefore close after us
    ;; this will only finish when the channel is closed
    (when (:read-model component)
      (close! (:channel event-channel))
      (<!! (:event-listener component)))
    component))

(defn read-model-component []
  (map->ReadModelComponent {}))
