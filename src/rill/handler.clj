(ns rill.handler
  (:require [clojure.tools.logging :as log]
            [rill.aggregate :as aggregate :refer [update-aggregate]]
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
        [aggregate current-version] (retrieve-aggregate-and-version event-store id)
        additional-ids (aggregate/aggregate-ids aggregate command)
        additional-aggregates (map #(retrieve-aggregate event-store %) additional-ids)]
    (into [id current-version aggregate] additional-aggregates)))

(declare notify-observers)

(defn notify-observer
  [event-store event observer-id handler-fn primary]
  (let [observer (retrieve-aggregate event-store observer-id)
        rest-aggregates (map #(retrieve-aggregate event-store %) (aggregate/aggregate-ids primary event))
        triggered-events (seq (apply handler-fn observer event primary rest-aggregates))]
    (when (and triggered-events
               (commit-events event-store observer-id any-stream-version triggered-events))
      (let [new-observer (update-aggregate observer (filter #(= observer-id (message/primary-aggregate-id %)) triggered-events))]
        (concat triggered-events (mapcat #(notify-observers event-store % new-observer) triggered-events))))))

(defn notify-observers
  [event-store event primary]
  (mapcat (fn [[observer-id handler-fn]]
            (notify-observer event-store event observer-id handler-fn primary))
          (message/observers event)))

(defn try-command
  [event-store command]
  (let [[id version & [primary-aggregate & rest-aggregates]] (prepare-aggregates event-store command)]
    (if (and (contains? command :expected-version)
             (not= version (:expected-version command)))
      [:out-of-date {:expected-version (:expected-version command) :actual-version version}]
      (let [[status events :as response] (apply aggregate/handle-command primary-aggregate command rest-aggregates)]
        (case status
          :ok (if (commit-events event-store id version events)
                (let [new-primary (update-aggregate primary-aggregate (filter #(= (message/primary-aggregate-id %) id) events))
                      triggered (mapcat #(notify-observers event-store % new-primary) events)]
                  [:ok events (+ version (count events)) triggered])
                [:conflict])
          :rejected response)))))
