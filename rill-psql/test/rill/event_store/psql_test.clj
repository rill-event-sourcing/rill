(ns rill.event-store.psql-test
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store :as store]
            [rill.event-stream :as stream]
            [rill.message :as message :refer [defevent]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [clojure.core.async :as async]
            [rill.event-channel :refer [event-channel]]
            [schema.core :as s]))

(defevent TestEvent
  :v s/Int)

(defn clear-db!
  [spec]
  (jdbc/db-do-commands spec "TRUNCATE rill_events")
  (while (not= 0 (:count (first (jdbc/query spec "SELECT COUNT(*) AS count FROM rill_events"))))
    (Thread/sleep 50)))

(deftest test-psql-event-store
  (when-let [uri (env :psql-event-store-uri)]
    (clear-db! uri)
    (let [store (psql-event-store uri)
          events (map test-event (range 7))]
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

(def big-blob (repeat 1000 (repeat 1000 "BLOB")))

(deftest test-all-events-stream
  (when-let [uri (env :psql-event-store-uri)]
    (testing "sequential appends"
      (clear-db! uri)
      (let [store (psql-event-store uri)
            stream-ids (repeatedly 4 new-id)
            events (map test-event (range 100))]
        (dorun (map (fn [id es]
                      (is (store/append-events store id -1 es)))
                    stream-ids (partition-all 25 events)))
        (is (= (map :v events)
               (map :v (store/retrieve-events store stream/all-events-stream-id))))))

    (testing "concurrent appends"
      (clear-db! uri)
      (let [store (psql-event-store uri)
            big-id (new-id)
            small-id (new-id)
            big-chan (async/thread
                       (dorun (map-indexed (fn [i e]
                                             (store/append-events store big-id (dec i) [e]))
                                           (map (fn [i]
                                                  (test-event {:big i
                                                               :blob big-blob})) (range 100)))))
            small-chan (async/thread
                         (dorun (map-indexed (fn [i e]
                                               (store/append-events store small-id (dec i) [e]))
                                             (map (fn [i]
                                                    (test-event {:small i}))
                                                  (range 1000)))))
            listener-chan (event-channel store stream/all-events-stream-id -1 0)
            out (loop [channels [big-chan small-chan listener-chan]
                       counts {:big 0
                               :small 0}]
                  (let [[e c] (async/alts!! channels)]
                    (if e
                      (recur channels (cond (:big (:v e))
                                            (update-in counts [:big] inc)
                                            (:small (:v e))
                                            (update-in counts [:small] inc)
                                            :else
                                            counts))
                      (let [new-chans (vec (remove #(= c %) channels))]
                        (if (= 1 (count new-chans))
                          (do (async/close! listener-chan)
                              counts)
                          (recur new-chans counts))))))]
        (is (= out {:big 100
                    :small 1000}))))))

