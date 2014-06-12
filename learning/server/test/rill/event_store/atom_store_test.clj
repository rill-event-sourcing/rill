(ns rill.event-store.atom-store-test
  (:require [rill.event-store.atom-store-test.local :refer [with-local-atom-store]]
            [rill.event-store :as store]
            [rill.message :refer [defevent]]
            [rill.event-stream :refer [empty-stream-version]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]
            [clojure.core.async :as async :refer [<!!]]
            [rill.event-channel :refer [event-channel]]))

(defevent TestAtomEvent [stream-id])
(def stream-id (new-id))

(defn gen-event
  []
  (->TestAtomEvent (new-id) stream-id))

(is (= (rill.message/strict-map->Message "TestAtomEvent" {:id "1" :stream-id"2"})
       (->TestAtomEvent "1" "2")))

(def events (repeatedly 340 gen-event))
(def additional-events (repeatedly 331 gen-event))

(def polling-events (repeatedly 445 gen-event))


(deftest test-atom-store
  (with-local-atom-store [store]
    (testing "appending and retrieving events"
      (is (store/append-events store stream-id empty-stream-version events))
      (is (= (store/retrieve-events store stream-id)
             events))

      (is (store/append-events store stream-id (count events) additional-events))
      (is (= (store/retrieve-events store stream-id)
             (concat events additional-events)))
      (is (= (store/retrieve-events-since store stream-id (count events) 0)
             additional-events)))

    (testing "Long polling"
      (let [from-version (+ (count events) (count additional-events))
            post (async/thread
                   (Thread/sleep 2)
                   (store/append-events store stream-id from-version polling-events))
            poll (async/thread
                   (store/retrieve-events-since store stream-id from-version 4))]
        (is (<!! post))
        (is (<!! poll) polling-events)))

    (testing "rough throughput, with overhead"
      (let [from-version (+ (count events) (count additional-events) (count polling-events))
            read-buffer-size 10
            write-chunk-size 10
            num-messages 10000
            throughput-events (repeatedly num-messages gen-event)
            recieve (event-channel store stream-id from-version 10)]
        (println "Writing and recieving" num-messages "messages")
        (time
         (let [post (async/thread
                      (reduce (fn [version chunk]
                                (store/append-events store stream-id version chunk)
                                (+ version (count chunk)))
                              from-version
                              (partition-all write-chunk-size throughput-events)))]
           (doseq [e throughput-events]
             (is (= (<!! recieve) e)))
           (async/close! recieve)
           (is (= (+ num-messages from-version) (<!! post)))))))))

