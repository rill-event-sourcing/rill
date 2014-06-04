(ns studyflow.util
  (:require [clojure.string :as str]
            [schema.core :as s]))

(defn uuid-coercion-matcher
  [schema]
  (when (= schema s/Uuid)
    (fn [data]
      (if (string? data)
        (java.util.UUID/fromString data)
        data))))

