(ns rill.event-store.memory
  "An in-memory event store for testing purposes"
  (:require [rill.event-store :as store]
            [rill.event-stream :as stream]
            [slingshot.slingshot :refer [try+ throw+]]))

(deftype MemoryStore [state]
  store/EventStore
  (retrieve-events-since [this stream-id from-version wait-for-seconds]
    (loop [wait wait-for-seconds]
      (let [substream (subvec (get @state stream-id stream/empty-stream) (inc from-version))]
        (if (empty? substream)
          (if (< 0 wait-for-seconds)
            (do (Thread/sleep 200)
                (recur (dec wait)))
            substream)
          substream))))

  (append-events [this stream-id from-version events]
    (try+ (swap! state (fn [old-state]
                         (let [current-stream (get old-state stream-id stream/empty-stream)]
                           (if (= (dec (count current-stream)) from-version)
                             (assoc old-state stream-id (into current-stream events))
                             (throw+ ::out-of-date)))))
          true
          (catch #(= % ::out-of-date) err
            nil))))

(defn memory-store
  []
  (MemoryStore. (atom {})))
