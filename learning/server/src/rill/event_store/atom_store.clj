(ns rill.event-store.atom-store
  "This is an event store implementation using the GetEventstore.com store as a backend.

Code originally taken from https://github.com/jankronquist/rock-paper-scissors-in-clojure/tree/master/eventstore/src/com/jayway/rps
"
  (:require [clojure.tools.logging :as log]
            [rill.event-store :refer [EventStore]]
            [rill.event-store.atom-store.cursor :as cursor]
            [rill.event-store.atom-store.event :as event]
            [rill.message :as msg]))

(defrecord UnprocessableMessage [v])

(defn safe-convert
  [m]
  (try
    (msg/strict-map->Message (:type m) (dissoc m :type))
    (catch RuntimeException e
      (log/info "Ignoring malformed event" e)
      (->UnprocessableMessage m))))

(defn to-atom-event
  [e]
  (assoc e
    :type (msg/type e)
    :id (msg/id e)))

(deftype AtomStore [uri constructor]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (cursor/event-seq (if (= -1 cursor)
                        (cursor/first-cursor uri stream-id)
                        (cursor/next-cursor cursor))
                      constructor
                      wait-for-seconds))
  (append-events [_ stream-id from-version events]
    (event/post uri stream-id from-version (map to-atom-event events))))

(defn atom-event-store
  ([uri message-constructor]
     (->AtomStore uri message-constructor))
  ([uri]
     (atom-event-store uri safe-convert)))

