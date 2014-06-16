(ns rill.uuid
  (:import java.util.UUID))

(defn new-id
  []
  (str (UUID/randomUUID)))

(defn uuid
  [u]
  (if (string? u)
    (UUID/fromString u)
    u))
