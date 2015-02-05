(ns rill.event-store.psql
  (:require [clojure.core.async :refer [thread <!! >!! chan close!]]
            [rill.event-store :refer [EventStore]]
            [rill.event-stream :refer [all-events-stream-id]]
            [clojure.java.jdbc :as sql]
            [rill.message :as message]
            [clojure.tools.logging :as log]))

(defn record->message
  [thaw r]
  (with-meta (assoc (thaw (:payload r))
                    ::message/number (:stream_order r))
    {:cursor (or (:insert_order r)
                 (:stream_order r))}))

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
    (sql/query spec ["SELECT payload, stream_order FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY stream_order ASC LIMIT ?"
                     (str stream-id)
                     cursor page-size])))

(defn select-all-fn
  [spec]
  (fn [cursor page-size]
    (retrying-query spec ["SELECT payload, stream_order, insert_order FROM rill_events WHERE insert_order > ? ORDER BY insert_order ASC LIMIT ?"
                          cursor page-size])))

(defn messages
  [thaw cursor page-size selector]
  (let [p (map #(record->message thaw %) (selector cursor page-size))]
    (if (< (count p) page-size)
      (seq p) ;; make sure we return nil when no messages are found
      (concat p (lazy-seq (messages thaw (:cursor (meta (last p))) page-size selector))))))

(defn unique-violation?
  "true when the exception was caused by a unique constraint violation"
  [sql-exception]
  (= (.getSQLState sql-exception) "23505"))

(defn catch-up-events [spec]
  (log/info "Catch-up-events")
  (let [page-size 100 ;; arbitrary
        in (chan 3)
        out (chan 3)]
    ;; db thread
    (thread
      (log/info "[db] Check which insert_order the database is at (and count events)")
      (let [{watermark :max_insert_order
             row-count :row_count} (first (retrying-query spec ["SELECT MAX(insert_order) as max_insert_order, COUNT(*) as row_count FROM rill_events"]))]
        (log/info "[db] Event-store max :insert_order " watermark " and row-count: " row-count)
        (loop [cursor -1]
          (let [rows (retrying-query spec ["SELECT payload, stream_order, insert_order FROM rill_events WHERE insert_order > ? ORDER BY insert_order ASC LIMIT ?"
                                           cursor page-size]
                                     :result-set-fn vec)
                highest-insert-order (:insert_order (peek rows))]
            (>!! in rows)
            (log/info "[db] Passed along upto :insert_order" highest-insert-order " cursor: " cursor)
            (if (< (count rows) page-size)
              (log/info "[db] Got a batch with less than page-size events, done with reading db [watermark old-cursor highest-insert-order]" [watermark cursor highest-insert-order])
              (recur highest-insert-order))))
        (log/info "[db] Caught up now, double check the watermarks")
        (let [new-watermark (:max_insert_order (first (retrying-query spec ["SELECT MAX(insert_order) as max_insert_order FROM rill_events"])))]
          (log/info "[db] new watermark: " new-watermark " number of events since starting catch-up: " (- new-watermark watermark))))
      (log/info "[db] Finished catching up from db")
      (close! in))

    ;; deserializer thread
    (thread
      (log/info "[deserializer] Starting deserializer")
      (loop []
        (when-let [rows (<!! in)]
          (log/info "[deserializer] Deserializing from " (:insert_order (first rows)) " upto " (:insert_order (peek rows)))
          (let [transformed (mapv record->message rows)]
            (>!! out transformed))
          (recur)))
      (close! out)
      (log/info "[deserializer] Stopped deserializer"))

    ;; lazy-seq
    (let [out-seq (fn out-seq []
                    (if-let [events (<!! out)]
                      (concat events (lazy-seq (out-seq)))
                      (do (log/info "[lazy-seq] Ending lazy-seq from catch-up events")
                          nil)))]
      (log/info "[lazy-seq] Delivering to lazy-seq")
      (out-seq))
    ))

(defrecord PsqlEventStore [spec page-size freeze thaw]
  EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds]
    (if (and (= stream-id all-events-stream-id)
             (= cursor -1))
      ;; catching up case
      (catch-up-events spec)
      ;; listening after catching up and aggregate case
      (let [cursor (if (number? cursor)
                     cursor
                     (or (message/number cursor)
                         (throw (ex-info (str "Not a valid cursor: " cursor) {:cursor cursor}))))]
        (or (messages thaw cursor page-size
                      (if (= stream-id all-events-stream-id)
                        (select-all-fn spec)
                        (select-stream-fn spec stream-id)))
            (do (when (< 0 wait-for-seconds)
                  (Thread/sleep 200))
                [])))))

  (append-events [this stream-id from-version events]
    (if (= from-version -2) ;; generate our own stream_order
      (apply  sql/db-do-prepared spec "INSERT INTO rill_events (event_id, stream_id, stream_order, payload) VALUES (?, ?, (SELECT(COALESCE(MAX(stream_order),-1)+1) FROM rill_events WHERE stream_id=?), ?)"
              (map-indexed (fn [i e]
                             [(str (message/id e))
                              (str stream-id)
                              (str stream-id)
                              (freeze e)])
                           events))
      (try (apply sql/db-do-prepared spec "INSERT INTO rill_events (event_id, stream_id, stream_order, payload) VALUES (?, ?, ?, ?)"
                  (map-indexed (fn [i e]
                                 [(str (message/id e))
                                  (str stream-id)
                                  (+ 1 i from-version)
                                  (freeze e)])
                               events))
           true
           (catch java.sql.BatchUpdateException e
             (when-not (unique-violation? e)
               (throw e))
             false))))) ;; conflict - there is already an event with the given stream_order

(defn psql-event-store [spec {:keys [freeze thaw] :as serializer} & [{:keys [page-size] :or {page-size 20}}]]
  {:pre [(integer? page-size) serializer freeze thaw]}
  (let [es (->PsqlEventStore spec page-size freeze thaw)]
    (assoc es :store es)))
