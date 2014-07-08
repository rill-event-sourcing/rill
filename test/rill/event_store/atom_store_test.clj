(ns rill.event-store.atom-store-test
  (:require [rill.event-store.atom-store-test.local :refer [with-local-atom-store]]
            [rill.event-store :as store]
            [rill.message :as message :refer [defevent]]
            [rill.event-stream :refer [empty-stream-version empty-stream]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [clojure.core.async :as async :refer [<!!]]
            [rill.event-channel :refer [event-channel]]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

(defevent TestAtomEvent
  :num s/Int)

(def stream-id (new-id))

(defn gen-event
  ([i]
     (->TestAtomEvent (str (new-id)) i))
  ([]
     (gen-event -666)))

(def events (repeatedly 49 gen-event))
(def additional-events (repeatedly 2 gen-event))

(def polling-events (repeatedly 309 gen-event))

(def throughput-stream-id (new-id))

(deftest test-atom-store
  (with-local-atom-store [store]
    (testing "appending and retrieving events"
      (testing "reading an empty stream"
        (is (= (store/retrieve-events store (new-id))
               empty-stream)
            "Reading an empty stream returns the empty stream"))
      
      (is (store/append-events store stream-id empty-stream-version events))
      (let [retrieved-events (store/retrieve-events store stream-id)]
        (is (= (->> retrieved-events
                    (map #(dissoc % message/number))) events))
        (is (store/append-events store stream-id (dec (count events)) additional-events))
        (is (= (->> (store/retrieve-events store stream-id)
                    (map #(dissoc % message/number))) (concat events additional-events)))
        (is (= (->> (store/retrieve-events-since store stream-id (:cursor (meta (last retrieved-events))) 0)
                    (map #(dissoc % message/number)))
               additional-events))))

    (testing "Long polling"
      (let [earlier (store/retrieve-events store stream-id)
            cursor (:cursor (meta (last earlier)))
            post (async/thread
                   (Thread/sleep 2)
                   (try
                     (store/append-events store stream-id (dec (count earlier)) polling-events)
                     (catch Exception e
                       (log/error e)
                       (.printStackTrace e)
                       (throw e))))
            poll (async/thread
                   (try
                     (store/retrieve-events-since store stream-id cursor 4)
                     (catch Exception e
                       (log/error e)
                       (.printStackTrace e)
                       (throw e))))]
        (is (<!! post))
        (is (<!! poll) polling-events)))

    (testing "rough throughput, with overhead"
      (let [read-buffer-size 10
            write-chunk-size 12
            num-messages 1000
            throughput-events (map gen-event (range num-messages))
            recieve (event-channel store throughput-stream-id -1 10)]
        (println "Writing and recieving" num-messages "messages")
        (time
         (let [post (async/thread
                      (let [r (reduce (fn [version chunk]
                                        (store/append-events store throughput-stream-id version chunk)
                                        (+ version (count chunk)))
                                      -1
                                      (partition-all write-chunk-size throughput-events))]
                        (log/debug "done appending" (inc r) "messages")
                        r))]
           (doseq [[e i] (map vector throughput-events (range num-messages))]
             (is (= (dissoc (<!! recieve) message/number) e)))
           (async/close! recieve)
           (is (= (<!! post) (dec (count throughput-events))))))))))
