(ns rill.temp-store
  (:require [rill.event-store :refer [retrieve-events]]
            [rill.aggregate :refer [load-aggregate]]
            [rill.handler :refer [try-command]]
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
