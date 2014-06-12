(ns rill.event-store.memory-test
  (:require [rill.event-store :as store]
            [rill.event-store.memory :as memory]
            [rill.event-stream :as stream]
            [clojure.test :refer [is deftest testing]]))

(deftest in-memory-event-store
  (let [store (memory/memory-store)]
    (is (= (store/retrieve-events store "foo") stream/empty-stream)
            "retrieving a non-existing stream returns the empty stream")

    (is (store/append-events store "my-stream" stream/empty-stream-version [:a :b :c :d])
        "can push onto a non-existing stream using the empty stream")

    (is (not (store/append-events store "my-stream" stream/empty-stream-version [:e :f]))
        "needs the current stream to add events to an existing stream")

    (let [s (store/retrieve-events store "my-stream")]
      (is (= s [:a :b :c :d])
          "returns successfully appended events in chronological order")
      (is (store/append-events store "my-stream" (+ stream/empty-stream-version (count s)) [:e :f]))
      (is (= (store/retrieve-events store "my-stream") [:a :b :c :d :e :f])))

    (let [s (store/retrieve-events store "my-other-stream")]
      (testing "event store handles each stream independently"
        (is (= s stream/empty-stream))
          (is (store/append-events store "my-other-stream" (+ stream/empty-stream-version (count s)) [:e :f]))
          (is (= (store/retrieve-events store "my-other-stream") [:e :f]))
          (is (= (store/retrieve-events store "my-stream") [:a :b :c :d :e :f]))))))

