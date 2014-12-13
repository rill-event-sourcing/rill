(ns rill.event-store.atom-store
  "This is an event store implementation using the GetEventstore.com store as a backend.

Code originally taken from https://github.com/jankronquist/rock-paper-scissors-in-clojure/tree/master/eventstore/src/com/jayway/rps
"
  (:require [clojure.tools.logging :as log]
            [rill.event-store :refer [EventStore]]
            [rill.event-store.atom-store.cursor :as cursor]
            [rill.event-store.atom-store.event :as event]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as msg]
            [clojure.string :as string]))


(defn client-opts
  [user password]
  {:pre [user password]}
  (if (and user password)
    {:basic-auth [user password]}))

(defn sanitize-stream-name
  [stream-id]
  (string/replace (str stream-id) #"[^a-zA-Z0-9_\-]" "_"))

(deftype AtomStore [uri user password]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (cursor/event-seq (if (= -1 cursor)
                        (cursor/first-cursor uri (if (= all-events-stream-id stream-id)
                                                   "%24all"
                                                   (sanitize-stream-name stream-id))
                                             (client-opts user password))
                        (cursor/next-cursor cursor))
                      wait-for-seconds))
  (append-events [_ stream-id from-version events]
    (event/post uri (sanitize-stream-name stream-id) from-version events (client-opts user password))))

(defn atom-event-store
  [uri & [{:keys [user password]}]]
  {:pre [user password uri]}
  (->AtomStore uri user password))
