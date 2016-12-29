(ns rill.event-store.mysql
  (:require [clojure.java.jdbc :as jdbc]
            [rill.event-store :refer [EventStore retrieve-events-since append-events]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message]
            [clojure.core.cache :as cache]
            [taoensso.nippy :as nippy]))

(defn- select-all
  [spec cursor page-size]
  {:pre [(integer? cursor)]}
  (jdbc/with-db-transaction [tr spec {:read-only? true}]
    (jdbc/query tr ["SELECT rill_streams.stream_id AS stream_id, insert_order, stream_order, payload, event_type, created_at FROM rill_events JOIN rill_streams ON (rill_events.stream_number=rill_streams.stream_number) WHERE insert_order > ? ORDER BY insert_order ASC LIMIT ?" cursor page-size])))

(defn- all-record->message
  [{:keys [payload insert_order stream_order event_type created_at stream_id]}]
  (-> (nippy/thaw payload)
      (assoc :rill.message/number stream_order
             :rill.message/cursor insert_order
             :rill.message/type (keyword event_type)
             :rill.message/stream-id stream_id)
      (update :rill.message/timestamp #(or % created_at))))

(defn- select-stream
  [spec stream-number cursor page-size]
  (jdbc/with-db-transaction [tr spec {:read-only? true}]
    (jdbc/query tr ["SELECT stream_order, payload, event_type, created_at FROM rill_events WHERE stream_number = ? AND stream_order > ? ORDER BY stream_order ASC LIMIT ?" stream-number cursor page-size])))

(defn- stream-record->message-fn
  [stream-id]
  (fn [{:keys [payload stream_order event_type created_at]}]
    (-> (nippy/thaw payload)
        (assoc :rill.message/number stream_order
               :rill.message/cursor stream_order
               :rill.message/type (keyword event_type)
               :rill.message/stream-id stream-id)
        (update :rill.message/timestamp #(or % created_at)))))

(defn messages
  [cursor page-size f]
  (let [p (vec (f cursor page-size))
        c (count p)]
    (if (< c page-size)
      (when-not (zero? c)
        p) ;; return nil on empty result set
      (concat p (lazy-seq (messages (message/cursor (peek p)) page-size f))))))

(defn- strip-metadata
  [e]
  (dissoc e
          :rill.message/type
          :rill.message/number
          :rill.message/stream-id
          :rill.message/cursor))

(defn- message->payload
  [m]
  (nippy/freeze (strip-metadata m)))

(defn- select-stream-number
  [spec stream-id]
  (first (jdbc/query spec ["SELECT stream_number FROM rill_streams WHERE stream_id=? LIMIT 1" stream-id]
                     {:row-fn :stream_number})))

(defn- stream-number-db!
  [spec stream-id]
  (or (select-stream-number spec stream-id)
      (do (jdbc/execute! spec ["INSERT IGNORE INTO rill_streams SET stream_id=?" stream-id])
          (select-stream-number spec stream-id))))

(defn- stream-number!
  [spec stream-id !cache]
  (or (-> (swap! !cache (fn [c]
                          (if (cache/has? c stream-id)
                            (cache/hit c stream-id)
                            c)))
          (get stream-id))
      (let [num (stream-number-db! spec stream-id)]
        (swap! !cache (fn [c] (cache/miss c stream-id num)))
        num)))

(defrecord MysqlEventStore [spec page-size stream-num-cache]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (let [cursor (if (integer? cursor)
                   cursor
                   (:rill.message/cursor cursor))]
      (or (if (= all-events-stream-id stream-id)
            (messages cursor page-size #(sequence (map all-record->message)
                                                  (select-all spec %1 %2)))
            (let [stream-num (stream-number! spec stream-id stream-num-cache)]
              (messages cursor page-size #(sequence (map (stream-record->message-fn (str stream-id)))
                                                    (select-stream spec stream-num %1 %2)))))
          (do (when (pos? wait-for-seconds)
                (Thread/sleep 200))
              []))))
  (append-events [_ stream-id from-version events]
    (let [stream-id     (str stream-id)
          stream-number (stream-number! spec stream-id stream-num-cache)]
      (jdbc/with-db-transaction [tr spec]
        (try
          (jdbc/execute! tr ["LOCK TABLES rill_events WRITE"])
          (let [max-version (or (first (jdbc/query tr ["SELECT MAX(stream_order) AS max FROM rill_events WHERE stream_number=?" stream-number]
                                                   {:row-fn :max}))
                                -1)]
            (when (or (= from-version -2)
                      (= max-version from-version))
              (jdbc/insert-multi! tr :rill_events
                                  [:stream_number :stream_order :payload :event_type]
                                  (map-indexed (fn [index event]
                                                 [stream-number
                                                  (+ index 1 max-version)
                                                  (message->payload event)
                                                  (subs (str (:rill.message/type event)) 1)])
                                               events))
              true))
          (finally
            (jdbc/execute! tr ["UNLOCK TABLES"])))))))

(defn mysql-event-store
  ([db-spec]
   (mysql-event-store db-spec 1000 (cache/lru-cache-factory {} :threshold 10000)))
  ([db-spec page-size stream-id-cache]
   (->MysqlEventStore db-spec page-size (atom stream-id-cache))))
