(ns rill.event-store.psql-test
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store :as store]
            [rill.event-stream :as stream]
            [rill.message :as message :refer [defevent]]
            [rill.uuid :refer [new-id]]
            [rill.event-store.psql-util :as util]
            [clojure.test :refer :all]
            [clojure.core.async :as async]
            [rill.event-channel :refer [event-channel]]
            [schema.core :as s]))

(def uri util/event-store-uri)

(def store (psql-event-store uri))

(defevent TestEvent
          :v s/Int)

(defn init-db! [f]
  (if-not (util/table-exists? "rill_events")
    (util/load-schema!)
    (util/clear-db!))
  (f))

(use-fixtures :once init-db!)

(deftest test-psql-event-store
  (let [events (map test-event (range 7))
        other-events (map test-event (range 3))]
    (is (= (store/retrieve-events store "foo") stream/empty-stream)
        "retrieving a non-existing stream returns the empty stream")

    (is (store/append-events store "my-stream" stream/empty-stream-version (take 3 events))
        "can push onto a non-existing stream using the empty stream")

    (is (not (store/append-events store "my-stream" stream/empty-stream-version (drop 3 events)))
        "needs the current stream to add events to an existing stream")

    (let [s (store/retrieve-events store "my-stream")]
      (is (= (map #(dissoc % message/number message/cursor message/stream-id) s)
             (take 3 events))
          "returns successfully appended events in chronological order")

      (is (store/append-events store "my-stream" (+ stream/empty-stream-version (count s)) (drop 3 events)))
      (is (= (->> (store/retrieve-events store "my-stream")
                  (map #(dissoc % message/number message/cursor message/stream-id))) events)))

    (let [s (store/retrieve-events store "my-other-stream")]
      (testing "event store handles each stream independently"
        (is (= s stream/empty-stream))
        (is (store/append-events store "my-other-stream" stream/empty-stream-version other-events))
        (is (= (->> (store/retrieve-events store "my-other-stream")
                    (map #(dissoc % message/number message/cursor message/stream-id))) other-events))
        (is (= (->> (store/retrieve-events store "my-stream")
                    (map #(dissoc % message/number message/cursor message/stream-id))) events))))

    (is (= (map message/number (store/retrieve-events store "my-stream"))
           (map message/cursor (store/retrieve-events store "my-stream"))
           (range (count events)))
        "incremental message numbers from 0 ...")

    (let [e (nth (store/retrieve-events store "my-stream") 3)]
      (is (= (->> (store/retrieve-events-since store "my-stream" e 0)
                  (map #(dissoc % message/number message/cursor message/stream-id)))
             (drop 4 events))))))

(def big-blob (repeat 1000 (repeat 1000 "BLOB")))

(deftest test-all-events-stream
  (testing "sequential appends"
    (util/clear-db!)
    (let [stream-ids (repeatedly 4 new-id)
          events (map test-event (range 100))]
      (dorun (map (fn [id es]
                    (is (store/append-events store id -1 es)))
                  stream-ids (partition-all 25 events)))
      (Thread/sleep 1000)
      (is (= (map :v events)
             (map :v (store/retrieve-events store stream/all-events-stream-id))))))

  (testing "concurrent mix of large and small events"
    (util/clear-db!)
    (let [big-id (new-id)
          small-id (new-id)
          big-chan (async/thread
                     (dorun (map-indexed (fn [i e]
                                           (store/append-events store big-id (dec i) [e]))
                                         (map (fn [i]
                                                (test-event {:big i
                                                             :blob big-blob})) (range 100))))
                     (Thread/sleep 5000))
          small-chan (async/thread
                       (dorun (map-indexed (fn [i e]
                                             (store/append-events store small-id (dec i) [e]))
                                           (map (fn [i]
                                                  (test-event {:small i}))
                                                (range 1000))))
                       (Thread/sleep 5000))
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
                        counts
                        (recur new-chans counts))))))]
      (is (= out {:big 100
                  :small 1000}))))

  (testing "many concurrent small events"
    (util/clear-db!)
    (let [num-streams 20
          events-per-stream 1000
          total-events (* num-streams events-per-stream)
          stream-ids (map #(str "stream-" %) (range num-streams))
          insert-chans (map-indexed (fn [stream-num stream-id]
                                      (async/thread
                                        (dotimes [i events-per-stream]
                                          (is (store/append-events store stream-id
                                                                   (if (even? stream-num) (dec i) stream/any-stream-version)
                                                                   [(test-event [stream-id i])])))))
                                    stream-ids)
          listener-chan (event-channel store stream/all-events-stream-id -1 0)
          _ (println "Sending" (* num-streams events-per-stream) "events...")
          update-counts (fn [counts e]
                          (if (vector? (:v e))
                            (-> counts
                                (update-in [(first (:v e))] inc)
                                (assoc (str "last-seen-" (first (:v e))) (second (:v e)))
                                (update-in [:total] inc))
                            counts))
          update-previous (fn [prev {[stream-id num :as v] :v}]
                            (if (vector? v)
                              (do (is (= {:stream stream-id :num (dec num)}
                                         {:stream stream-id :num (prev stream-id)})
                                      "consecutive events in source stream")
                                  (assoc prev stream-id num))
                              prev))
          [out previous] (loop [channels (vec (cons listener-chan insert-chans))
                                counts (into {:total 0} (map #(vector % 0) stream-ids))
                                previous (into {} (map #(vector % -1) stream-ids))]
                           (let [[e c] (async/alts!! channels)]
                             (if e
                               (recur channels (update-counts counts e) (update-previous previous e))
                               (let [new-chans (vec (remove #(= c %) channels))]
                                 (if (= 1 (count new-chans))
                                   [counts previous]
                                   (recur new-chans counts previous))))))
          _ (println "Inserted all events, waiting for last" (- total-events (:total out)) "events")
          out (loop [channels [(async/timeout (* 30 1000)) listener-chan]
                     counts out
                     previous previous]
                (let [[e c] (async/alts!! channels)]
                  (if e
                    (let [new-counts (update-counts counts e)]
                      (if (= (:total new-counts) total-events)
                        new-counts
                        (recur channels new-counts (update-previous previous e))))
                    counts)))]
      (is (= out (into {:total (* events-per-stream num-streams)}
                       (mapcat #(vector [% events-per-stream]
                                        [(str "last-seen-" %) (dec events-per-stream)])
                               stream-ids))))))

  (testing "many concurrent small events in chunks"
    (util/clear-db!)
    (let [num-streams 20
          events-per-stream 1000
          total-events (* num-streams events-per-stream)
          events-per-chunk 8
          stream-ids (map #(str "stream-" %) (range num-streams))
          insert-chans (map-indexed (fn [stream-num stream-id]
                                      (async/thread
                                        (dotimes [i (/ events-per-stream events-per-chunk)]
                                          (is (store/append-events store stream-id
                                                                   (if (even? stream-num) (dec (* i events-per-chunk)) stream/any-stream-version)
                                                                   (mapv (fn [chunk-i]
                                                                           (test-event [stream-id (+ (* events-per-chunk i) chunk-i)]))
                                                                         (range events-per-chunk)))))))
                                    stream-ids)
          listener-chan (event-channel store stream/all-events-stream-id -1 0)
          _ (println "Sending" (* num-streams events-per-stream) "events...")
          update-counts (fn [counts e]
                          (if (vector? (:v e))
                            (-> counts
                                (update-in [(first (:v e))] inc)
                                (assoc (str "last-seen-" (first (:v e))) (second (:v e)))
                                (update-in [:total] inc))
                            counts))
          update-previous (fn [prev {[stream-id num :as v] :v}]
                            (if (vector? v)
                              (do (is (= {:stream stream-id :num (dec num)}
                                         {:stream stream-id :num (prev stream-id)})
                                      "consecutive events in source stream")
                                  (assoc prev stream-id num))
                              prev))
          [out previous] (loop [channels (vec (cons listener-chan insert-chans))
                                counts (into {:total 0} (map #(vector % 0) stream-ids))
                                previous (into {} (map #(vector % -1) stream-ids))]
                           (let [[e c] (async/alts!! channels)]
                             (if e
                               (recur channels (update-counts counts e) (update-previous previous e))
                               (let [new-chans (vec (remove #(= c %) channels))]
                                 (if (= 1 (count new-chans))
                                   [counts previous]
                                   (recur new-chans counts previous))))))
          _ (println "Inserted all events, waiting for last" (- total-events (:total out)) "events")
          out (loop [channels [(async/timeout (* 30 1000)) listener-chan]
                     counts out
                     previous previous]
                (let [[e c] (async/alts!! channels)]
                  (if e
                    (let [new-counts (update-counts counts e)]
                      (if (= (:total new-counts) total-events)
                        new-counts
                        (recur channels new-counts (update-previous previous e))))
                    counts)))]
      (is (= out (into {:total (* events-per-stream num-streams)}
                       (mapcat #(vector [% events-per-stream]
                                        [(str "last-seen-" %) (dec events-per-stream)])
                               stream-ids)))))))
