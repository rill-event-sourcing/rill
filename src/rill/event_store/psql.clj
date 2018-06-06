(ns rill.event-store.psql
  (:require [clojure.core.async :refer [thread <!! >!! chan close!]]
            [rill.event-store :refer [EventStore]]
            [rill.event-stream :refer [all-events-stream-id]]
            [clojure.java.jdbc :as sql]
            [rill.message :as message]
            [taoensso.nippy :as nippy]
            [rill.uuid :refer [uuid]]
            [clojure.tools.logging :as log])
  (:import [java.util Date]
           [java.sql Timestamp SQLException]))

(defn record->metadata
  [r]
  (let [m (transient {message/number (:stream_order r)
                      message/cursor (or (:insert_order r)
                                         (:stream_order r))})
        typed (if-let [type-str (:event_type r)]
                (assoc! m message/type (keyword type-str))
                m)
        stamped (if-let [timestamp ^Timestamp (:created_at r)]
                  (assoc! typed message/timestamp (Date. (.getTime timestamp)))
                  typed)
        streamed (if-let [stream-id (:stream_id r)]
                  (assoc! stamped message/stream-id stream-id)
                   stamped)]
    (persistent! streamed)))

(defn record->message
  [r]
  (merge (-> r :payload nippy/thaw)
         (record->metadata r)))

(defn wrap-auto-retry
  [f pause-seconds]
  (fn [& args]
    (loop [args args]
      (let [r (try
                (apply f args)
                (catch Exception e
                  (log/error e (str "Exception, will retry in " pause-seconds" seconds."))
                  ::retry))]
        (if (= r ::retry)
          (do (Thread/sleep (* 1000 pause-seconds))
              (recur args))
          r)))))

(def retrying-query (wrap-auto-retry sql/query 30))

(defn select-stream-fn
  [spec stream-id]
  (fn [cursor page-size]
    (mapv #(dissoc % :insert_order)
          (sql/query spec ["SELECT * FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY stream_order ASC LIMIT ?"
                           (str stream-id)
                           cursor page-size]))))

(defn select-all-fn
  [spec]
  (fn [cursor page-size]
    (retrying-query spec ["SELECT * FROM rill_events WHERE insert_order > ? AND insert_order IS NOT NULL ORDER BY insert_order ASC LIMIT ?"
                          cursor page-size])))

(defn messages
  [cursor page-size selector]
  (let [p (mapv record->message (selector cursor page-size))]
    (if (< (count p) page-size)
      (if (= 0 (count p))
        nil  ;; make sure we return nil when no messages are found
        p)
      (concat p (lazy-seq (messages (message/cursor (peek p)) page-size selector))))))

(defn unique-violation?
  "true when the exception was caused by a unique constraint violation"
  [sql-exception]
  (= (.getSQLState sql-exception) "23505"))

(defn catch-up-events [spec]
  (log/debug "Catch-up-events")
  (let [page-size 100 ;; arbitrary
        in (chan 3)
        out (chan 3)]
    ;; db thread
    (thread
      (log/debug "[db] Check which insert_order the database is at (and count events)")
      (let [{watermark :max_insert_order
             row-count :row_count} (first (retrying-query spec ["SELECT MAX(insert_order) as max_insert_order, COUNT(*) as row_count FROM rill_events"]))]
        (log/debug "[db] Event-store max :insert_order " watermark " and row-count: " row-count)
        (loop [cursor -1]
          (let [rows (retrying-query spec ["SELECT * FROM rill_events WHERE insert_order > ? AND insert_order IS NOT NULL ORDER BY insert_order ASC LIMIT ?"
                                           cursor page-size]
                                     {:result-set-fn vec})
                highest-insert-order (:insert_order (peek rows))]
            (>!! in rows)
            (log/debug "[db] Passed along upto :insert_order" highest-insert-order " cursor: " cursor)
            (if (< (count rows) page-size)
              (log/debug "[db] Got a batch with less than page-size events, done with reading db [watermark old-cursor highest-insert-order]" [watermark cursor highest-insert-order])
              (recur highest-insert-order))))
        (log/debug "[db] Caught up now, double check the watermarks")
        (let [new-watermark (:max_insert_order (first (retrying-query spec ["SELECT MAX(insert_order) as max_insert_order FROM rill_events"])))]
          (log/debug "[db] new watermark: " new-watermark " number of events since starting catch-up: " (- (or new-watermark 0) (or watermark 0)))))
      (log/debug "[db] Finished catching up from db")
      (close! in))

    ;; deserializer thread
    (thread
      (log/debug "[deserializer] Starting deserializer")
      (loop []
        (when-let [rows (<!! in)]
          (log/debug "[deserializer] Deserializing from " (:insert_order (first rows)) " upto " (:insert_order (peek rows)))
          (let [transformed (mapv record->message rows)]
            (>!! out transformed))
          (recur)))
      (close! out)
      (log/debug "[deserializer] Stopped deserializer"))

    ;; lazy-seq
    (let [out-seq (fn out-seq []
                    (if-let [events (<!! out)]
                      (concat events (lazy-seq (out-seq)))
                      (do (log/debug "[lazy-seq] Ending lazy-seq from catch-up events")
                          nil)))]
      (log/debug "[lazy-seq] Delivering to lazy-seq")
      (out-seq))))

(defn type->str
  [k]
  (subs (str k) 1))

(defn strip-metadata
  [e]
  (dissoc e message/type message/number message/timestamp message/stream-id message/cursor))

(defrecord PsqlEventStore [spec page-size]
  EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (if (and (= stream-id all-events-stream-id)
             (= cursor -1))
      ;; catching up case
      (catch-up-events spec)
      ;; listening after catching up and aggregate case
      (let [cursor (if (number? cursor)
                     cursor
                     (or (message/cursor cursor)
                         (throw (ex-info (str "Not a valid cursor: " cursor) {:cursor cursor}))))]
        (or (messages cursor page-size
                      (if (= stream-id all-events-stream-id)
                        (select-all-fn spec)
                        (select-stream-fn spec stream-id)))
            (do (when (< 0 wait-for-seconds)
                  (Thread/sleep 200))
                [])))))

  (append-events [this stream-id from-version events]
    (try (if (= from-version -2) ;; generate our own stream_order
           (do (sql/with-db-transaction [conn spec]
                 (sql/db-do-prepared conn false (cons "INSERT INTO rill_events (stream_id, stream_order, event_type, payload) VALUES ( ?, (SELECT(COALESCE(MAX(stream_order),-1)+1) FROM rill_events WHERE stream_id=?), ?, ?)"
                                                      (map-indexed (fn [i e]
                                                                     [(str stream-id)
                                                                      (str stream-id)
                                                                      (type->str (message/type e))
                                                                      (nippy/freeze (strip-metadata e))])
                                                                   events))
                                     {:multi? true}))
               true)
           (try (sql/with-db-transaction [conn spec]
                  (sql/db-do-prepared conn (cons "INSERT INTO rill_events (stream_id, stream_order, event_type, payload) VALUES (?, ?, ?, ?)"
                                                 (map-indexed (fn [i e]
                                                                [(str stream-id)
                                                                 (+ 1 i from-version)
                                                                 (type->str (message/type e))
                                                                 (nippy/freeze (strip-metadata e))])
                                                              events))
                                      {:multi? true}))
                true
                (catch java.sql.BatchUpdateException e ;; conflict - there is already an event with the given stream_order
                  (when-not (unique-violation? e)
                    (throw e))
                  false)))
         (catch SQLException e
           (if-let [next-exception (.getNextException e)]
             (throw next-exception)
             (throw e))))))

(defn psql-event-store [spec & [{:keys [page-size] :or {page-size 20}}]]
  {:pre [(integer? page-size)]}
  (let [es (->PsqlEventStore spec page-size)]
    (assoc es :store es)))
