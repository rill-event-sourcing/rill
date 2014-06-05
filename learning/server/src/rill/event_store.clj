(ns rill.event-store)

(defprotocol EventStore
  (retrieve-event-stream [this aggregate-id])
  (append-events [this aggregate-id previous-event-stream events]))
