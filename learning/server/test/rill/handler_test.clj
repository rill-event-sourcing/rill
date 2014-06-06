(ns rill.handler-test
  (:require [rill.handler :as handler :refer [aggregate-ids defaggregate-ids handle-command try-command]]
            [rill.message :refer [defcommand defevent stream-id]]
            [clojure.test :refer [deftest testing is]]
            [rill.uuid :refer [new-id]]
            [rill.event-store :refer [retrieve-event-stream]]
            [rill.event-stream :refer [empty-stream]]
            [rill.event-store.memory :refer [memory-store]]))

(defcommand TestCommand1 [something my-id other-thing])
(defaggregate-ids TestCommand1 my-id)

(defcommand TestCommand2 [id-one something id-two other-thing])
(defaggregate-ids TestCommand2 id-one id-two)

(deftest aggregate-ids-test
  (is (= (aggregate-ids (->TestCommand1 12345 :a :b :c))
         [:b]))
  (is (= (aggregate-ids (->TestCommand2 12345 :a :b :c :d))
         [:a :c])))

(defcommand HandlerCommand [agg-id])
(defaggregate-ids HandlerCommand agg-id)

(defevent TestEvent [agg-id given-aggregate])
(defmethod handle-command HandlerCommand
  [command my-aggregate]
  [(->TestEvent (new-id) (:agg-id command) my-aggregate)])

(def my-aggregate-id 2798)

(deftest test-try-command
  (testing "we get events out of the command"
    (is (= (map class (handle-command (->HandlerCommand (new-id) :my-id) :foo))
           [TestEvent]))
    (is (= (:given-aggregate (first (handle-command (->HandlerCommand (new-id) :my-id) :foo)))
           :foo))
    (is (= (stream-id (->TestEvent (new-id) my-aggregate-id :foo))
           my-aggregate-id))
    (is (= (stream-id (first (handle-command (->HandlerCommand (new-id) my-aggregate-id) :foo)))
           my-aggregate-id)))

  (testing "preparation of command"
    (is (handler/prepare-aggregates (memory-store) (->HandlerCommand (new-id) :my-id))
        [:my-id empty-stream nil]))
  
  (testing "the events from a command handler get stored in the relevant aggregate stream"
    (let [store (memory-store)]
      (is (= (retrieve-event-stream store my-aggregate-id) empty-stream))
      (is (true? (try-command store (->HandlerCommand (new-id) my-aggregate-id))))
      (is (not= (retrieve-event-stream store my-aggregate-id) empty-stream))
      (is (= (map class (:events (retrieve-event-stream store my-aggregate-id)))
             [TestEvent])))))
