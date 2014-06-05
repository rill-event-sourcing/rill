(ns studyflow.t-schema-tools
  (:require [studyflow.schema-tools :as tools]
            [midje.sweet :refer :all]
            [schema.core :as s]
            [schema.coerce :refer [coercer] :as coerce]))


(def IdSchema
  {s/Uuid s/Str})

(def id-coercer (coercer IdSchema tools/uuid-coercion-matcher))

(fact "id coercer will tranform the keys to UUIDs"
      (id-coercer {"9dd02654-1fd8-43f4-8de4-25a821eadf0d" "9dd02654-1fd8-43f4-8de4-25a821eadf0d"}) =>
      {#uuid "9dd02654-1fd8-43f4-8de4-25a821eadf0d" "9dd02654-1fd8-43f4-8de4-25a821eadf0d"})

(fact
 (count (tools/matching-coercers {s/Uuid s/Keyword} [tools/uuid-coercion-matcher coerce/json-coercion-matcher])) => 2)



(def JsonSchema
  {:an-id s/Uuid
   :a-number s/Int})

(def full-coercer (coercer JsonSchema tools/schema-coercion-matcher))

(fact "schema coercer will transform numbers, keywords and UUIDs"
      (full-coercer  {"an-id" "9dd02654-1fd8-43f4-8de4-25a821eadf0d"
                      "a-number" "1234"}) =>
      {:an-id #uuid "9dd02654-1fd8-43f4-8de4-25a821eadf0d"
       :a-number 1234})

