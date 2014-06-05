(ns studyflow.schema-tools
  (:require [schema.core :as s]
            [schema.coerce :as coerce])
  (:import (java.util UUID)))

(defn uuid-coercion-matcher
  [schema]
  (when (= schema s/Uuid)
    (fn [data]
      (if (string? data)
        (UUID/fromString data)
        data))))

(defn matching-coercers
  "return matching coercers for the given schema and matchers, or nil"
  [schema matchers]
  (seq (keep (fn [m] (m schema)) matchers)))

(defn combine-coercion-matchers
  "compose coercers from each matching matcher"
  [& matchers]
  (fn [schema]
    (if-let [coercers (matching-coercers schema matchers)]
      (apply comp coercers))))

(def schema-coercion-matcher
  "A matcher that coerces keywords, uuids and keyword enums from
  strings, and longs and doubles from numbers (without losing precision)"
  (combine-coercion-matchers uuid-coercion-matcher coerce/json-coercion-matcher))
