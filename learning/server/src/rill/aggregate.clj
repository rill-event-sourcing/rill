(ns rill.aggregate
  (:require [rill.message :as message]))

(defmulti handle-event
  "Take an event and return the new state of the aggregate"
  (fn [aggregate event]
    [(class aggregate) (message/type event)]))

(defn update-aggregate
  [aggregate events]
  (reduce handle-event aggregate events))

(defn load-aggregate
  [events]
  (update-aggregate nil events))

(defmacro defaggregate [name attrs]
  `(defrecord ~name ~(into '[id] attrs)))


