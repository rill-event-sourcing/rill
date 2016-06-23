(ns rill.handler-test
  (:require [rill.handler :as handler :refer [try-command]]
            [rill.aggregate :refer [handle-command aggregate-ids handle-event]]
            [rill.message :as message :refer [defcommand defevent primary-aggregate-id observers]]
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

(defmethod handle-event ::HandlerTestEvent
  [agg event]
  agg)

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
    (is (= (handler/prepare-aggregates (given []) (handler-command :primary-agg-id))
           [:primary-agg-id empty-stream-version nil])))

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

(defevent FooEvent
  :id s/Uuid)

(defevent BarEvent
  :id s/Uuid)

(defevent BazEvent
  :id s/Uuid)

(defmethod handle-event ::FooEvent
  [agg event]
  agg)

(defmethod handle-event ::BarEvent
  [agg event]
  agg)

(defmethod handle-event ::BazEvent
  [agg event]
  agg)

(defcommand FooCommand
  :id s/Uuid)

(defcommand BarCommand
  :id s/Uuid)

(defmethod handle-command ::FooCommand
  [agg cmd]
  [:ok [(foo-event (:id cmd))]])

(defmethod handle-command ::BarCommand
  [agg cmd]
  [:ok [(bar-event (:id cmd))]])

(defmethod observers ::FooEvent
  [_]
  [[:foo-observer (fn [observer event primary & rest-aggregates]
                    [(bar-event (new-id))])]])

(defmethod observers ::BarEvent
  [_]
  [[:bar-observer (fn [observer event primary & rest-aggregates]
                    [(baz-event (new-id))])]])

(deftest test-observers
  (testing "Events triggered by command"
    (let [[_ _ _ triggered-events] (try-command (given []) (bar-command (new-id)))]
      ;; BarCommand -> BarEvent -> BarEventObserver -> BazEvent
      (is (= [::BazEvent] (map message/type triggered-events)))))

  (testing "Events triggered by other triggered events"
    (let [[_ _ _ triggered-events] (try-command (given []) (foo-command (new-id)))]
      ;; FooCommand -> FooEvent -> FooEventObserver -> BarEvent -> BarEventObserver -> BazEvent
      (is (= [::BarEvent ::BazEvent] (map message/type triggered-events))))))
