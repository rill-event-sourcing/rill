(ns rill.event-store.psql
  (:require [rill.event-store :refer [EventStore]]
            [rill.event-stream :refer [all-events-stream-id]]
            [clojure.java.jdbc :as sql]
            [rill.message :as message]
            [taoensso.nippy :as nippy]
            [clojure.tools.logging :as log]))

(defrecord PsqlEventStore [spec]
  EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (let [cursor (if (number? cursor)
                   cursor
                   (or (message/number cursor)
                       (throw (ex-info (str "Not a valid cursor: " cursor) {:cursor cursor}))))]
      (or (seq (map (fn [r]
                      (with-meta (assoc (nippy/thaw (:payload r))
                                   ::message/number (or (:stream_order r)
                                                        (:insert_order r)))
                        {:cursor (or (:stream_order r)
                                     (:insert_order r))}))
                    (if (= stream-id all-events-stream-id)
                      (sql/query spec ["SELECT payload, insert_order FROM rill_events WHERE insert_order > ? ORDER BY insert_order ASC"
                                       cursor])
                      (sql/query spec ["SELECT payload, stream_order FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY insert_order ASC"
                                       (str stream-id)
                                       cursor]))))
          (do (when (< 0 wait-for-seconds)
                (Thread/sleep 200))
              []))))

  (append-events [this stream-id from-version events]
    (if (= from-version -2) ;; generate our own stream_order
      (apply  sql/db-do-prepared spec "INSERT INTO rill_events (event_id, stream_id, stream_order, payload) VALUES (?, ?, (SELECT(COALESCE(MAX(stream_order),-1)+1) FROM rill_events WHERE stream_id=?), ?)"
              (map-indexed (fn [i e]
                             [(str (message/id e))
                              (str stream-id)
                              (str stream-id)
                              (nippy/freeze e)])
                           events))
      (try (apply sql/db-do-prepared spec "INSERT INTO rill_events (event_id, stream_id, stream_order, payload) VALUES (?, ?, ?, ?)"
                  (map-indexed (fn [i e]
                                 [(str (message/id e))
                                  (str stream-id)
                                  (+ 1 i from-version)
                                  (nippy/freeze e)])
                               events))
           true
           (catch org.postgresql.util.PSQLException e
             nil)
           (catch java.sql.BatchUpdateException e
             nil)))))

(defn psql-event-store [spec]
  (let [es (->PsqlEventStore spec)]
    (assoc es :store es)))
