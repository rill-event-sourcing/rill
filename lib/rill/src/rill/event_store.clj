(ns rill.event-store)

(defprotocol EventStore
  (retrieve-events-since [this stream-id cursor wait-for-seconds])
  (append-events [this stream-id from-version events]))

(defn retrieve-events
  [store stream-id]
  (retrieve-events-since store stream-id -1 0))

