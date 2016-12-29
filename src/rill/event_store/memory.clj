(ns rill.event-store.memory
  "An in-memory event store for testing purposes"
  (:require [clojure.tools.logging :as log]
            [rill.event-store :as store]
            [rill.event-stream :refer [all-events-stream-id empty-stream-version any-stream-version empty-stream]]
            [rill.message :as message]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import (java.util Date)))

(defn with-cursors
  [c events]
  (map-indexed #(assoc %2 :rill.message/cursor (+ c %1)) events))

(deftype MemoryStore [state]
  store/EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (let [cursor (or (:rill.message/cursor cursor) cursor)]
      (loop [wait wait-for-seconds]
        (let [substream (subvec (get @state stream-id empty-stream) (inc cursor))]
          (if (empty? substream)
            (if (< 0 wait-for-seconds)
              (do (Thread/sleep 200)
                  (recur (dec wait)))
              (with-cursors (inc cursor) substream))
            (with-cursors (inc cursor) substream))))))

  (append-events [this stream-id from-version events]
    (try+ (swap! state (fn [old-state]
                         (let [current-stream  (get old-state stream-id empty-stream)
                               all-stream      (get old-state all-events-stream-id empty-stream)
                               current-version (if (empty? current-stream)
                                                 empty-stream-version
                                                 (message/number (last current-stream)))
                               start-number    (if (= from-version any-stream-version)
                                                 current-version
                                                 from-version)
                               events          (map (fn [e i]
                                                      (-> e
                                                          (assoc message/number (+ start-number i))
                                                          (update message/timestamp #(or % (Date.)))))
                                                    events (iterate inc 1))]
                           (if (= (dec (count current-stream)) start-number)
                             (-> old-state
                                 (assoc stream-id (into current-stream events))
                                 (assoc all-events-stream-id
                                        (into all-stream (map #(assoc % :rill.message/stream-id stream-id)) events)))
                             (throw+ ::out-of-date)))))
          true
          (catch #(= % ::out-of-date) err
            nil))))

(defn memory-store
  []
  (MemoryStore. (atom {})))
