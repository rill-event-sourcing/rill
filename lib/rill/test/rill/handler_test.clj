(ns rill.handler-test
  (:require [rill.handler :as handler :refer [try-command]]
            [rill.aggregate :refer [handle-command aggregate-ids]]
            [rill.message :as message :refer [defcommand defevent primary-aggregate-id]]
            [clojure.test :refer [deftest testing is]]
            [rill.uuid :refer [new-id]]
            [rill.event-store :refer [retrieve-events]]
            [rill.event-stream :refer [empty-stream empty-stream-version]]
            [rill.temp-store :refer [given]]
            [schema.core :as s]))

(defcommand HandlerCommand
  :agg-id s/Uuid)

(defevent HandlerTestEvent
  :agg-id s/Uuid
  :given-aggregate s/Keyword)

(defmethod handle-command ::HandlerCommand
  [my-aggregate command]
  [:ok [(handler-test-event (:agg-id command) my-aggregate)]])

(def my-aggregate-id 2798)

(deftest test-try-command
  (testing "we get events out of the command"
    (is (= (map message/type (second (handle-command :foo (handler-command :my-id))))
           [::HandlerTestEvent]))
    (is (= (:given-aggregate (first (second (handle-command :foo (handler-command :my-id)))))
           :foo))
    (is (= (primary-aggregate-id (handler-test-event my-aggregate-id :foo))
           my-aggregate-id))
    (is (= (primary-aggregate-id (first (second (handle-command :foo (handler-command my-aggregate-id)))))
           my-aggregate-id)))

  (testing "preparation of command"
    (is (handler/prepare-aggregates (given []) (handler-command :my-id))
        [:my-id empty-stream-version nil]))

  (testing "the events from a command handler get stored in the relevant aggregate stream"
    (let [store (given [])]
      (is (= empty-stream
             (retrieve-events store my-aggregate-id)))
      (is (= :ok
             (first (try-command store (handler-command my-aggregate-id)))))
      (is (not= empty-stream
                (retrieve-events store my-aggregate-id)))
      (is (= [::HandlerTestEvent]
             (map message/type (retrieve-events store my-aggregate-id)))))))
