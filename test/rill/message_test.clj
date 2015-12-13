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

(def bar-message
  (message/message :msg/BarMessage
    :my-id s/Int))

(deftest test-defmessage
  (testing "generated constructors"
    (is (= (message/type (foo-message 456))
           ::FooMessage))
    (is (= (:my-id (foo-message 456))
           456))
    (is (= (message/type (bar-message 123))
           :msg/BarMessage))
    (is (= (:my-id (bar-message 123))
           123))

    (is (message/id (foo-message 1)))
    (is (not= (message/id (foo-message 1))
              (message/id (foo-message 1))))
    (is (message/timestamp (foo-message 2)))
    (let [t1 (message/timestamp (foo-message 1))]
      (Thread/sleep 22)
      (let [t2 (message/timestamp (foo-message 1))]
        (is (not= t1 t2))))))
