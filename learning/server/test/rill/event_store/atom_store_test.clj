(ns rill.event-store.atom-store-test
  (:require [rill.event-store.atom-store-test.local :refer [with-local-atom-store]]
            [rill.event-store :as store]
            [rill.message :refer [defevent]]
            [rill.event-stream :refer [empty-stream-version]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [clojure.core.async :as async :refer [<!!]]
            [rill.event-channel :refer [event-channel]]
            [clojure.tools.logging :as log]))

(defevent TestAtomEvent [stream-id])
(def stream-id (new-id))

(defn gen-event
  []
  (->TestAtomEvent (str (new-id)) (str stream-id)))

(is (= (rill.message/strict-map->Message "TestAtomEvent" {:id "1" :stream-id"2"})
       (->TestAtomEvent "1" "2")))

(def events (repeatedly 49 gen-event))
(def additional-events (repeatedly 52 gen-event))

(def polling-events (repeatedly 309 gen-event))

(def throughput-stream-id (new-id))

(deftest test-atom-store
  (with-local-atom-store [store]
    (testing "appending and retrieving events"
      (is (store/append-events store stream-id empty-stream-version events))
      (let [retrieved-events (store/retrieve-events store stream-id)]
        (is (= retrieved-events events))
        (is (store/append-events store stream-id (dec (count events)) additional-events))
        (is (= (store/retrieve-events store stream-id) (concat events additional-events)))
        (is (= (store/retrieve-events-since store stream-id (:cursor (meta (last retrieved-events))) 0)
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

    #_(testing "rough throughput, with overhead"
      (let [read-buffer-size 10
            write-chunk-size 10
            num-messages 40
            throughput-events (repeatedly num-messages gen-event)
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
             (prn "waiting for event " i e)
             (is (= (<!! recieve) e))
             (prn "recieved"))
           (async/close! recieve)
           (is (= (<!! post) (dec (count throughput-events))))))))))
