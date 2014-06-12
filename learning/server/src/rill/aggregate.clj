(ns rill.aggregate)

(defmulti handle-event
  "Take an event and return the new state of the aggregate"
  (fn [aggregate event]
    [(class aggregate) (class event)]))

(defn update-aggregate
  [aggregate events]
  (reduce handle-event aggregate events))

(defn load-aggregate
  [events]
  (update-aggregate nil events))

(defmacro defaggregate [name attrs]
  `(defrecord ~name ~(into '[id] attrs)))


