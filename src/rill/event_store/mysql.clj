(ns rill.event-store.mysql
  (:require [clojure.java.jdbc :as jdbc]
            [rill.event-store :refer [EventStore retrieve-events-since append-events]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message]
            [taoensso.nippy :as nippy]))

(defn select-all
  [spec cursor page-size]
  (jdbc/query spec ["SELECT stream_id, insert_order, stream_order, payload, event_type FROM rill_events WHERE insert_order > ? ORDER BY insert_order LIMIT ?" cursor page-size]))

(defn all-record->message
  [{:keys [payload insert_order stream_order event_type created_at stream_id]}]
  (-> (nippy/thaw payload)
      (assoc ::message/number stream_order
             ::message/cursor insert_order
             ::message/type (keyword event_type)
             ::message/stream-id stream_id)))

(defn select-stream
  [spec stream-id cursor page-size]
  (jdbc/query spec ["SELECT stream_order, payload, event_type FROM rill_events WHERE stream_id = ? AND stream_order > ? ORDER BY stream_order LIMIT ?" stream-id cursor page-size]))

(defn stream-record->message-fn
  [stream-id]
  (fn [{:keys [payload stream_order event_type created_at]}]
    (-> (nippy/thaw payload)
        (assoc ::message/number stream_order
               ::message/cursor stream_order
               ::message/type (keyword event_type)
               ::message/stream-id stream-id))))

(defn strip-metadata
  [e]
  (dissoc e
          ::message/type
          ::message/number
          ::message/stream-id
          ::message/cursor))

(defn message->payload
  [m]
  (nippy/freeze (strip-metadata m)))

(defrecord MysqlEventStore [spec page-size]
  EventStore
  (retrieve-events-since [_ stream-id cursor wait-for-seconds]
    (let [cursor (if (integer? cursor)
                   cursor
                   (::message/cursor cursor))]
      (if (= all-events-stream-id stream-id)
        (sequence (map all-record->message)
                  (select-all spec cursor page-size))
        (sequence (map (stream-record->message-fn stream-id))
                  (select-stream spec stream-id cursor page-size)))))
  (append-events [_ stream-id from-version events]
    (jdbc/with-db-transaction [tr spec]
      (try
        (jdbc/execute! tr ["LOCK TABLES rill_events WRITE"])
        (let [max-version (or (first (jdbc/query tr ["SELECT MAX(stream_order) AS max FROM rill_events WHERE stream_id=?" stream-id]
                                                    {:row-fn       :max
                                                     :transaction? false}))
                              -1)]
          (when (or (= from-version -2)
                    (= max-version from-version))
            (jdbc/insert-multi! tr :rill_events
                                [:stream_id :stream_order :payload :event_type]
                                (map-indexed (fn [index event]
                                               [stream-id
                                                (+ index 1 max-version)
                                                (message->payload event)
                                                (subs (str (::message/type event)) 1)])
                                             events)
                                {:transaction? false})))
        (finally
          (jdbc/execute! tr ["UNLOCK TABLES"]))))))

(defn mysql-event-store
  [spec]
  (->MysqlEventStore spec 100))
