(ns rill.temp-store
  (:require [rill.event-store :refer [retrieve-events append-events]]
            [rill.aggregate :refer [load-aggregate]]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.event-store.memory :refer [memory-store]]))

(defmacro with-temp-store
  "execute body with bindings [store fetch execute]
  `store` (a temporary in-memory event store)
, `fetch` (which loads an aggregate given an id)
  `execute!` executes a command."
  [[store fetch execute! :as bindings] & body]
  `(let [store# (memory-store)
         ~store store#
         ~fetch #(load-aggregate (retrieve-events store# %))
         ~execute! #(first (try-command store# %))]
     ~@body))

(defn execute
  "apply command to a store with given-events, return the [status,
  generated-events] pair"
  [command given-events]
  (let [store (memory-store)]
    (doseq [events (partition-by message/primary-aggregate-id given-events)]
      (append-events store (message/primary-aggregate-id (first events)) nil events))
    (try-command store command)))

(defn message=
  "test messages for equality ignoring rill.message/id"
  [& events]
  (= (map #(dissoc % message/id) events)))

(defn ignore-event-ids
  [events]
  (map #(dissoc % message/id) events))

(defn messages=
  [expected-events actual-events]
  (= (ignore-event-ids expected-events)
     (ignore-event-ids actual-events)))

(defn command-result=
  [[expected-status expected-events]
   [actual-status actual-events]]
  (and (= expected-status actual-status)
       (messages= expected-events actual-events)))
