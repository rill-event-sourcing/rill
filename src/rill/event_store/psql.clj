(ns rill.event-store.psql
  (:require [rill.event-store :refer [EventStore]]
            [clojure.java.jdbc :as sql]
            [rill.message :as message]
            [miner.tagged :as tagged]))

(defrecord PsqlEventStore [spec]
  EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (or (map (fn [r]
               (assoc (tagged/read-string (:payload r))
                 ::message/number (:stream_order r)))
             (sql/query spec ["SELECT payload, stream_order FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY insert_order ASC" stream-id
                              (if (number? cursor)
                                cursor
                                (or (message/number cursor)
                                    (throw (ex-info (str "Not a valid cursor: " cursor) {:cursor cursor}))))]))
        (do (Thread/sleep 1000)
            nil)))
  (append-events [this stream-id from-version events]
    (try
      (sql/with-db-transaction [t spec]
        (doseq [[i e] (map-indexed vector events)]
          (sql/insert! t "rill_events" {:event_id (message/id e)
                                        :stream_id stream-id
                                        :stream_order (+ 1 i from-version)
                                        :payload (pr-str e)})))
      true
      (catch org.postgresql.util.PSQLException e
        nil))))

(defn psql-event-store [spec]
  (->PsqlEventStore spec))
