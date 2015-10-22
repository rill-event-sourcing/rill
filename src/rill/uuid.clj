(ns rill.uuid
  (:import java.util.UUID))

(defn new-id
  []
  (UUID/randomUUID))

(defn uuid
  [u]
  (cond
    (string? u) (UUID/fromString u)
    (keyword? u) (UUID/fromString (name u))
    :else u))
