(ns rill.event-store.memory
  "An in-memory event store for testing purposes"
  (:require [rill.event-store :as store]
            [rill.event-stream :as stream]
            [slingshot.slingshot :refer [try+ throw+]]))


(defn add-to-stream
  [{:keys [version events]} new-events]
  (stream/->EventStream (inc version) (into events new-events)))

(deftype MemoryStore [state]
  store/EventStore
  (retrieve-event-stream [this aggregate-id]
    (get @state aggregate-id stream/empty-stream))
  (append-events [this aggregate-id previous-event-stream events]
    (try+ (swap! state (fn [old-state]
                         (let [current-stream (get old-state aggregate-id stream/empty-stream)]
                           (if (= current-stream previous-event-stream)
                             (assoc old-state aggregate-id (add-to-stream current-stream events))
                             (throw+ ::out-of-date)))))
          true
          (catch #(= % ::out-of-date) err
            nil))))

(defn memory-store
  []
  (MemoryStore. (atom {})))
