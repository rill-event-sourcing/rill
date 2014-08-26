(ns studyflow.migrations.wrap-migrations
  (:require [rill.event-store :refer [EventStore retrieve-events-since append-events]]))

(defrecord EventStoreWithMigrations [wrapped-event-store migrations]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (map migrations (retrieve-events-since wrapped-event-store stream-id cursor wait-for-seconds)))
  (append-events [_ stream-id from-version events]
    (append-events wrapped-event-store stream-id from-version events)))

(defn wrap-migrations
  "Return a new event store that applies migrations to every event read from wrapped-event-store"
  [wrapped-event-store migrations]
  (->EventStoreWithMigrations wrapped-event-store migrations))
