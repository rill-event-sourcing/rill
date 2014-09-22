(ns rill.event-store.psql
  (:require [rill.event-store :refer [EventStore]]
            [rill.event-stream :refer [all-events-stream-id]]
            [clojure.java.jdbc :as sql]
            [rill.message :as message]
            [taoensso.nippy :as nippy]
            [clojure.tools.logging :as log]))

(defn record->message
  [r]
  (with-meta (assoc (nippy/thaw (:payload r))
               ::message/number (:stream_order r))
    {:cursor (or (:insert_order r)
                 (:stream_order r))}))

(defn select-stream-fn
  [spec stream-id]
  (fn [cursor page-size]
    (sql/query spec ["SELECT payload, stream_order FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY insert_order ASC LIMIT ?"
                     (str stream-id)
                     cursor page-size])))

(defn select-all-fn
  [spec]
  (fn [cursor page-size]
    (sql/query spec ["SELECT payload, stream_order, insert_order FROM rill_events WHERE insert_order > ? ORDER BY insert_order ASC LIMIT ?"
                     cursor page-size])))

(defn messages
  [cursor page-size selector]
  (let [p (map record->message (selector cursor page-size))]
    (if (< (count p) page-size)
      (seq p) ;; make sure we return nil when no messages are found
      (concat p (lazy-seq (messages (:cursor (meta (last p))) page-size selector))))))

(defrecord PsqlEventStore [spec page-size]
  EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (let [cursor (if (number? cursor)
                   cursor
                   (or (message/number cursor)
                       (throw (ex-info (str "Not a valid cursor: " cursor) {:cursor cursor}))))]
      (or (messages cursor page-size
                    (if (= stream-id all-events-stream-id)
                      (select-all-fn spec)
                      (select-stream-fn spec stream-id)))
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
             (log/error e "append-events")
             (log/error (.getNextException e) "append-events getNextException")
             (throw e))
           (catch java.sql.BatchUpdateException e
             (log/error e "append-events")
             (log/error (.getNextException e) "append-events getNextException")
             (throw e))))))

(defn psql-event-store [spec & [{:keys [page-size] :or {page-size 20}}]]
  {:pre [(integer? page-size)]}
  (let [es (->PsqlEventStore spec page-size)]
    (assoc es :store es)))
