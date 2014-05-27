(ns rill.uuid
  (:import java.util.UUID))

(defn new-id
  []
  (str (UUID/randomUUID)))
