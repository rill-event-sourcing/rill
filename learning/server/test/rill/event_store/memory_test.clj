(ns rill.event-store.memory-test
  (:require [rill.event-store :as store]
            [rill.event-store.memory :as memory]
            [rill.event-stream :as stream]
            [rill.message :as message]
            [clojure.test :refer [is deftest testing]]))

(def events (map (fn [v] {:v v}) (range 7)))

(deftest in-memory-event-store
  (let [store (memory/memory-store)]
    (is (= (store/retrieve-events store "foo") stream/empty-stream)
        "retrieving a non-existing stream returns the empty stream")

    (is (store/append-events store "my-stream" stream/empty-stream-version (take 3 events))
        "can push onto a non-existing stream using the empty stream")

    (is (not (store/append-events store "my-stream" stream/empty-stream-version (drop 3 events)))
        "needs the current stream to add events to an existing stream")

    (let [s (store/retrieve-events store "my-stream")]
      (is (= (map #(dissoc % message/number) s)
             (take 3 events))
          "returns successfully appended events in chronological order")
      (is (store/append-events store "my-stream" (+ stream/empty-stream-version (count s)) (drop 3 events)))
      (is (= (->> (store/retrieve-events store "my-stream")
                  (map #(dissoc % message/number))) events)))

    (let [s (store/retrieve-events store "my-other-stream")]
      (testing "event store handles each stream independently"
        (is (= s stream/empty-stream))
        (is (store/append-events store "my-other-stream" (+ stream/empty-stream-version (count s)) (drop 3 events)))
        (is (= (->> (store/retrieve-events store "my-other-stream")
                    (map #(dissoc % message/number))) (drop 3 events)))
        (is (= (->> (store/retrieve-events store "my-stream")
                    (map #(dissoc % message/number))) events))))

    (let [e (nth (store/retrieve-events store "my-stream") 3)]
      (is (= (->> (store/retrieve-events-since store "my-stream" e 0)
                  (map #(dissoc % message/number)))
             (drop 4 events))))))
