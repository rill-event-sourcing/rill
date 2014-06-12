(ns studyflow.schema-tools
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils]
            [slingshot.slingshot :refer [throw+]])
  (:import (java.util UUID)))

(defn uuid-coercion-matcher
  [schema]
  (when (= schema s/Uuid)
    (fn [data]
      (if (string? data)
        (UUID/fromString data)
        data))))

(defn schema-coercion-matcher
  "A matcher that coerces keywords, uuids and keyword enums from
  strings, and longs and doubles from numbers (without losing precision)"
  [schema]
  (some #(% schema) [uuid-coercion-matcher coerce/json-coercion-matcher]))

(defn strict-coercer
  [coercer]
  (fn [raw]
    (let [v (coercer raw)]
      (if (schema.utils/error? v)
        (throw+ (:error v))
        v))))
