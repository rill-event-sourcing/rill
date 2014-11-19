(ns rill.temp-store
  (:require [rill.event-store :refer [retrieve-events append-events]]
            [rill.aggregate :refer [load-aggregate]]
            [rill.repository :refer [wrap-basic-repository wrap-caching-repository]]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.event-stream :refer [any-stream-version]]
            [rill.event-store.memory :refer [memory-store]]))

(defmacro with-temp-store
  "execute body with bindings [store fetch execute]
  `store` (a temporary in-memory event store)
  , `fetch` (which loads an aggregate given an id)
  `execute!` executes a command."
  [[store fetch execute! :as bindings] & body]
  `(let [store# (wrap-basic-repository (memory-store))
         ~store store#
         ~fetch #(load-aggregate (retrieve-events store# %))
         ~execute! #(first (try-command store# %))]
     ~@body))

(defn given
  "return an event store/repository with the given events inserted"
  [given-events]
  (let [store (wrap-caching-repository (memory-store))]
    (doseq [events (partition-by message/primary-aggregate-id given-events)]
      (or (append-events store
                         (message/primary-aggregate-id (first events))
                         any-stream-version
                         events)
          (throw (ex-info "No events added" {:tried events}))))
    store))

(defn execute
  "apply command to a store with given-events, return the [status,
  generated-events] pair"
  [command given-events]
  (let [store (given given-events)]
    (try-command store command)))


(defn comparable-message
  "remove message id and number from a message, so it can be compared"
  [message]
  (dissoc message message/id message/number message/timestamp))

(defn message=
  "test messages for equality ignoring rill.message/id or rill.message/number"
  [& events]
  (apply = (map comparable-message events)))

(defn messages=
  [expected-events actual-events]
  (= (map comparable-message expected-events)
     (map comparable-message actual-events)))

(defn command-result=
  [[expected-status & expected-rest] [actual-status & actual-rest]]
  (if (= :ok expected-status actual-status)
    (let [[expected-events expected-triggers] expected-rest
          [actual-events _ actual-triggers] actual-rest]
      (and (messages= expected-events actual-events)
           (if expected-triggers
             (messages= expected-triggers actual-triggers)
             true)))
    (= expected-status actual-status)))
