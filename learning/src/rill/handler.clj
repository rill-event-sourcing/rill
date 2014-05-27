(ns rill.handler
  (:require [rill.message :as msg]
            [rill.aggregate :as aggregate]
            [rill.event-store :as store]))

(defn valid-commit?
  [[event & events]]
  ;; Every event must apply to the same aggregate root
  (and event
       (every? #(= (msg/aggregate-id event) (msg/aggregate-id %)) events)))

(defn validate-commit
  [events]
  (when-not (valid-commit? events)
    (throw (Exception. "Transactions must apply to exactly one aggregate"))))

(defn commit-events
  [store aggregate-id stream events]
  (validate-commit events)
  (let [aggregate-id-from-event (msg/aggregate-id (first events))]
    (if (= aggregate-id aggregate-id-from-event)
                                        ; events apply to current aggregate
      (store/append-events store aggregate-id stream events)
                                        ; events apply to newly created aggregate
      (store/append-events store aggregate-id-from-event nil events))))

(defn fetch-aggregate-and-stream
  [event-store id]
  (let [stream (store/retrieve-event-stream event-store id)
        aggregate (aggregate/load-aggregate (:events stream))]
    (pr stream)
    [aggregate stream]))

(defn fetch-aggregate
  [event-store id]
  (first (fetch-aggregate-and-stream event-store id)))

(defn try-command
  [event-store command]
  (let [id (msg/aggregate-id command)
        [aggregate stream] (fetch-aggregate-and-stream event-store id)
        additional-aggregates (map (fn [id] (fetch-aggregate event-store id))
                                   (msg/aggregate-ids command))]
    (if-let [events (apply aggregate/handle-command command (cons aggregate additional-aggregates))]
      (commit-events event-store id stream events)
      ::error)))

(defn make-handler [event-store]
  (fn [command]
    (try-command event-store command)))
