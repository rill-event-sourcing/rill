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
    (is (= (message/type (foo-message 456))
           ::FooMessage))
    (is (= (:my-id (foo-message 456))
           456))))

