(ns rill.handler
  (:require [clojure.tools.logging :as log]
            [rill.aggregate :as aggregate]
            [rill.event-store :as store]
            [rill.repository :refer [retrieve-aggregate-and-version retrieve-aggregate]]
            [rill.event-stream :as stream :refer [any-stream-version]]
            [rill.message :as message]))

(defn valid-commit?
  [[event & events]]
  ;; Every event must apply to the same aggregate root
  (and event
       (let [id (message/primary-aggregate-id event)]
         (every? #(= id (message/primary-aggregate-id %)) events))))

(defn validate-commit
  [events]
  (when-not (valid-commit? events)
    (throw (Exception. (format "Transactions must apply to exactly one aggregate. Given aggregate ids: %s"
                               (pr-str (map message/primary-aggregate-id  events)))))))

(defn commit-events
  [store stream-id from-version events]
  (validate-commit events)
  (log/debug ["committing events" events])
  (let [stream-id-from-event (message/primary-aggregate-id (first events))]
    (if (= stream-id stream-id-from-event)
                                        ; events apply to current aggregate
      (store/append-events store stream-id from-version events)
                                        ; events apply to newly created aggregate
      (store/append-events store stream-id-from-event stream/empty-stream-version events))))

(defn prepare-aggregates
  "fetch the primary event stream id and version and aggregates for `command`"
  [event-store command]
  (let [id (message/primary-aggregate-id command)
        additional-ids (aggregate/aggregate-ids command)
        [aggregate current-version] (retrieve-aggregate-and-version event-store id)
        additional-aggregates (map #(retrieve-aggregate event-store %) additional-ids)]
    (into [id current-version aggregate] additional-aggregates)))

(defn notify-process-manager
  [event-store event]
  (if-let [pm-id (message/process-manager-id event)]
    (let [pm (retrieve-aggregate event-store pm-id)
          primary (retrieve-aggregate event-store (message/primary-aggregate-id event))
          rest-aggregates (map #(retrieve-aggregate event-store %) (aggregate/aggregate-ids event))
          triggered-events (seq (apply aggregate/handle-notification pm event primary rest-aggregates))]
      (when (and triggered-events
                 (commit-events event-store pm-id any-stream-version triggered-events))
        (concat triggered-events (mapcat (partial notify-process-manager event-store) triggered-events))))))

(defn try-command
  [event-store command]
  (let [[id version & [primary-aggregate & rest-aggregates]] (prepare-aggregates event-store command)]
    (if (and (contains? command :expected-version)
             (not= version (:expected-version command)))
      [:out-of-date {:expected-version (:expected-version command) :actual-version version}]
      (let [[status events :as response] (apply aggregate/handle-command primary-aggregate command rest-aggregates)]
        (case status
          :ok (if (commit-events event-store id version events)
                (let [triggered (mapcat (partial notify-process-manager event-store) events)
                      all-events (concat events triggered)]
                  [:ok all-events (+ version (count (filter #(= id (message/primary-aggregate-id %)) all-events)))])
                [:conflict])
          :rejected response)))))

