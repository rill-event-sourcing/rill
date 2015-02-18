(ns rill.event-store.psql-test
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store :as store]
            [rill.event-stream :as stream]
            [rill.message :as message :refer [defevent]]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [schema.core :as s]))

(defevent TestEvent2
  :v s/Int)

(def events (map test-event2 (range 7)))

(deftest test-psql-event-store
  (when-let [uri (env :psql-event-store-uri)]
    (let [store (psql-event-store uri)]
      (is (= (store/retrieve-events store "foo") stream/empty-stream)
          "retrieving a non-existing stream returns the empty stream")

      (is (store/append-events store "my-stream" stream/empty-stream-version (take 3 events))
          "can push onto a non-existing stream using the empty stream")

      (is (not (store/append-events store "my-stream" stream/empty-stream-version (drop 3 events)))
          "needs the current stream to add events to an existing stream")

      (let [s (store/retrieve-events store "my-stream")]
        (is (= (map #(dissoc % message/number message/cursor) s)
               (take 3 events))
            "returns successfully appended events in chronological order")
        (is (store/append-events store "my-stream" (+ stream/empty-stream-version (count s)) (drop 3 events)))
        (is (= (->> (store/retrieve-events store "my-stream")
                    (map #(dissoc % message/number message/cursor))) events)))

      (let [s (store/retrieve-events store "my-other-stream")]
        (testing "event store handles each stream independently"
          (is (= s stream/empty-stream))
          (is (store/append-events store "my-other-stream" (+ stream/empty-stream-version (count s)) (drop 3 events)))
          (is (= (->> (store/retrieve-events store "my-other-stream")
                      (map #(dissoc % message/number message/cursor))) (drop 3 events)))
          (is (= (->> (store/retrieve-events store "my-stream")
                      (map #(dissoc % message/number message/cursor))) events))))

      (let [e (nth (store/retrieve-events store "my-stream") 3)]
        (is (= (->> (store/retrieve-events-since store "my-stream" e 0)
                    (map #(dissoc % message/number message/cursor)))
               (drop 4 events)))))))
