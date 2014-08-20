(ns rill.repository
  (:require [rill.event-store :refer [EventStore retrieve-events-since retrieve-events append-events]]
            [rill.aggregate :refer [update-aggregate load-aggregate handle-event]]
            [rill.message :as message]
            [clojure.core.cache :as cache]))

(defprotocol Repository
  (retrieve-aggregate-and-version [repository aggregate-id]))

(defn retrieve-aggregate
  [repository aggregate-id]
  (first (retrieve-aggregate-and-version repository aggregate-id)))

(defn assure-aggregate-atom-is-in-cache
  [state stream-id]
  (if (cache/has? state stream-id)
    (cache/hit state stream-id)
    (cache/miss state stream-id (atom [nil -1]))))

(defn get-or-make-aggregate-atom
  [cache stream-id]
  (get (swap! cache assure-aggregate-atom-is-in-cache stream-id) stream-id))

(defn update-aggregate-and-version
  [aggregate current-version events]
  (reduce (fn [[aggregate version] event]
            [(handle-event aggregate event) (message/number event)])
          [aggregate current-version]
          events))

(defn load-aggregate-and-version
  [events]
  (update-aggregate-and-version nil -1 events))

(defrecord CachingRepository [event-store cache]
  Repository
  (retrieve-aggregate-and-version [_ stream-id]
    (let [a (get-or-make-aggregate-atom cache stream-id)
          [aggregate version] @a
          new-state (update-aggregate-and-version aggregate version (retrieve-events-since event-store stream-id version 0))]
      (reset! a new-state)
      new-state))
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (retrieve-events-since event-store stream-id cursor wait-for-seconds))
  (append-events [_ stream-id from-version events]
    (append-events event-store stream-id from-version events)))

(defn wrap-caching-repository [event-store]
  (->CachingRepository event-store (atom (cache/lru-cache-factory  {} :threshold 20000))))

(defrecord BasicRepository [event-store]
  Repository
  (retrieve-aggregate-and-version [_ stream-id]
    (load-aggregate-and-version (retrieve-events event-store stream-id)))
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (retrieve-events-since event-store stream-id cursor wait-for-seconds))
  (append-events [_ stream-id from-version events]
    (append-events event-store stream-id from-version events)))

(defn wrap-basic-repository [event-store]
  (->BasicRepository event-store))

