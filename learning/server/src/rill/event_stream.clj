(ns rill.event-stream)

(defrecord EventStream [version events])

(def empty-stream (->EventStream -1 []))



