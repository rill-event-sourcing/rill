(ns studyflow.schema-tools-test
  (:require [studyflow.schema-tools :as tools]
            [clojure.test :refer [is deftest testing]]
            [schema.core :as s]
            [schema.coerce :refer [coercer] :as coerce]))

(def IdSchema
  {s/Uuid s/Str})

(def id-coercer (coercer IdSchema tools/uuid-coercion-matcher))

(deftest id-coercer-test
  (testing "id coercer will tranform the keys to UUIDs"
    (is (= (id-coercer {"9dd02654-1fd8-43f4-8de4-25a821eadf0d" "9dd02654-1fd8-43f4-8de4-25a821eadf0d"}))
        {#uuid "9dd02654-1fd8-43f4-8de4-25a821eadf0d" "9dd02654-1fd8-43f4-8de4-25a821eadf0d"})))

(def JsonSchema
  {:an-id s/Uuid
   :a-number s/Int
   :a-key s/Keyword})

(def full-coercer (coercer JsonSchema tools/schema-coercion-matcher))

(deftest schema-matcher-test
  (testing "schema coercer will transform numbers, keywords and UUIDs"
    (is (= (full-coercer  {:an-id "9dd02654-1fd8-43f4-8de4-25a821eadf0d"
                           :a-number 1234
                           :a-key "keyw"})
           {:an-id #uuid "9dd02654-1fd8-43f4-8de4-25a821eadf0d"
            :a-number 1234
            :a-key :keyw}))))
