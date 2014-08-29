(ns studyflow.components.event-channel
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :refer [info]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]))

(defrecord EventChannelComponent [event-store num-chans]
  Lifecycle
  (start [component]
    (info "Starting event-channel")
    (let [input-channel (event-channel (:store event-store) all-events-stream-id -1 0)
          m (async/mult input-channel)
          output-channels (vec (repeatedly num-chans async/chan))]
      (doseq [out output-channels]
        (async/tap m out))
      (assoc component
        :input-channel input-channel
        :output-channels output-channels)))
  (stop [component]
    (info "Stopping event-channel")
    (when-let [c (:input-channel component)]
      (async/close! c))
    (doseq [c (:output-channels component)]
      (async/close! c))
    (dissoc component :input-channel :output-channels)))

(defn channel
  [component num]
  {:post [%]}
  (nth (:output-channels component) num))

(defn event-channel-component [num-chans]
  (map->EventChannelComponent {:num-chans num-chans}))
