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
(defrecord UntypedMessage [v])

(defn safe-convert
  [m]
  (if (:type m)
    (try
      (msg/strict-map->Message (:type m) (dissoc m :type))
      (catch RuntimeException e
        (log/info "Ignoring malformed event" e)
        (->UnprocessableMessage m)))
    (->UntypedMessage m)))

(defn to-atom-event
  [e]
  (assoc e
    :type (msg/type e)
    :id (msg/id e)))

(defn client-opts
  [user password]
  (if (and user password)
    {:basic-auth [user password]}))

(deftype AtomStore [uri user password constructor]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (cursor/event-seq (if (= -1 cursor)
                        (cursor/first-cursor uri stream-id (client-opts user password))
                        (cursor/next-cursor cursor))
                      constructor
                      wait-for-seconds))
  (append-events [_ stream-id from-version events]
    (event/post uri stream-id from-version (map to-atom-event events) (client-opts user password))))

(defn atom-event-store
  [uri & [opts]]
  (let [opts (merge {:constructor safe-convert} opts)]
    (->AtomStore uri (:user opts) (:password opts) (:constructor opts))))
