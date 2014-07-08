(ns rill.message-test
  (:require [rill.message :as message :refer [defmessage]]
            [clojure.test :refer [deftest is testing]]
            [schema.core :as s]))

(deftest test-internals
  (testing "params->args"
    (is (= (message/params->args [:my-id s/Int :foo s/Str])
           '[my-id foo]))))

(defmessage FooMessage
  :my-id s/Int)

(deftest test-defmessage
  (testing "generated constructors"
    (is (= (->FooMessage 123 456)
           {::message/type ::FooMessage
            ::message/id 123
            :my-id 456}))
    (is (= (:my-id (foo-message 456))
           456))))

