(ns studyflow.learning.system.components.read-model
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.components.event-channel :refer [channel]]
            [studyflow.learning.read-model :refer [empty-model]]
            [studyflow.learning.read-model.event-listener :refer [listen!]]
            [clojure.core.async :refer [close! <!!]]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord ReadModelComponent [event-channel num]
  Lifecycle
  (start [component]
    (info "Starting read-model")
    (let [read-model (atom empty-model)]
      (assoc component
        :read-model read-model
        :event-listener (listen! read-model (channel event-channel num)))))
  (stop [component]
    (info "Stopping read-model")
    ;; this has a dependency on the event-channel, which will start
    ;; before as and therefore close after us
    ;; this will only finish when the channel is closed
    (when (:read-model component)
      (close! (channel event-channel num))
      (<!! (:event-listener component)))
    component))

(defn read-model-component [channel-number]
  (map->ReadModelComponent {:num channel-number}))
