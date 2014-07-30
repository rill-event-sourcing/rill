(ns rill.aggregate
  (:require [rill.message :as message]))

(defmulti handle-event
  "Take an event and return the new state of the aggregate"
  (fn [aggregate event]
    (message/type event)))

(defn update-aggregate
  [aggregate events]
  (reduce handle-event aggregate events))

(defn load-aggregate
  [events]
  (update-aggregate nil events))

(defmulti handle-command
  "handle command given aggregates. returns [:ok events-seq] or [:rejected reason]"
  (fn [primary-aggregate command & aggregates]
    (message/type command)))

(defmulti aggregate-ids
  "given a command, return the ids of the additional aggregates that should be
  fetched before calling handle-command."
  (fn [command]
    (message/type command)))

(defmethod aggregate-ids :default
  [_]
  nil)

