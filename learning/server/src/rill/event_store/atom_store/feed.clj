(ns rill.event-store.atom-store.feed)

(defn head-uri
  [store-uri stream-id]
  (str store-uri "/streams/" stream-id))







