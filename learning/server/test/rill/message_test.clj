(ns rill.message-test
  (:require [rill.message :as message :refer [defmessage]]
            [clojure.test :refer [deftest is testing]]
            [schema.core :as s]))

(deftest test-internals
  (testing "camel->kebab"
    (is (= (message/->type-keyword 'FooBar!)
           :foo-bar!)))
  
  (testing "params->args"
    (is (= (message/params->args [:my-id s/Int :foo s/Str])
           '[my-id foo]))))

(deftest test-defmessage
  (defmessage FooMessage
    :my-id s/Int)

  (testing "generated constructors"
    (is (= (->FooMessage 123 456)
           {::message/type :foo-message
            ::message/id 123
            :my-id 456}))))
