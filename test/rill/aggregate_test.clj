(ns rill.aggregate-test
  (:require [rill.aggregate :refer [handle-command aggregate-ids]]
            [rill.message :as message :refer [defcommand defevent primary-aggregate-id]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [schema.core :as s]))

(defcommand HandlerTestCommand1
  :my-id s/Uuid
  :something s/Keyword
  :other-thing s/Keyword)

(defcommand HandlerTestCommand2
  :id-one s/Uuid
  :something s/Keyword
  :id-two s/Uuid
  :other-thing s/Keyword)

(defmethod aggregate-ids ::HandlerTestCommand2
  [agg command]
  (cons (:foo agg) (map command [:id-two])))

(defcommand HandlerCommand
  :agg-id s/Uuid)

(defevent HandlerTestEvent
  :agg-id s/Uuid
  :given-aggregate s/Keyword)

(defmethod handle-command ::HandlerCommand
  [my-aggregate command]
  [:ok [(handler-test-event (:agg-id command) my-aggregate)]])

(defn custom-ag-id
  [event]
  (str (:foo event) ":" (:bar event)))

(defevent CustomAggIdEvent
  :foo s/Uuid
  :bar s/Uuid
  custom-ag-id)

(deftest custom-aggregate-id
  (let [e (custom-agg-id-event "foo" "bar")]
    (is (= (custom-ag-id e)
           (primary-aggregate-id e)))))

(def my-aggregate-id 2798)

(deftest aggregate-ids-test
  (is (= (primary-aggregate-id (handler-test-command1 :a :b :c))
         :a))
  (is (= (aggregate-ids {} (handler-test-command1 :a :b :c))
         nil))
  (is (= (primary-aggregate-id (handler-test-command2 :a :b :c :d))
         :a))
  (is (= (aggregate-ids {:foo :bar} (handler-test-command2 :a :b :c :d))
         [:bar :c])))

(deftest command-handling
  (testing "we get events out of the command"
    (is (= (map message/type (second (handle-command :foo (handler-command :my-id))))
           [::HandlerTestEvent]))
    (is (= (:given-aggregate (first (second (handle-command :foo (handler-command :my-id)))))
           :foo))
    (is (= (primary-aggregate-id (handler-test-event my-aggregate-id :foo))
           my-aggregate-id))
    (is (= (primary-aggregate-id (first (second (handle-command :foo (handler-command my-aggregate-id)))))
           my-aggregate-id))))

