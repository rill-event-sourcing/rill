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
  (->TestAtomEvent (str (new-id)) (str stream-id)))

(is (= (rill.message/strict-map->Message "TestAtomEvent" {:id "1" :stream-id"2"})
       (->TestAtomEvent "1" "2")))

(def events (repeatedly 4 gen-event))
(def additional-events (repeatedly 2 gen-event))

(def polling-events (repeatedly 40 gen-event))


(deftest test-atom-store
  (with-local-atom-store [store]
    (testing "appending and retrieving events"
      (is (store/append-events store stream-id empty-stream-version events))
      (is (= (store/retrieve-events store stream-id)
             events))

      (is (store/append-events store stream-id (count events) additional-events))
      (is (= (store/retrieve-events store stream-id)
             (concat events additional-events)))
      (is (= (store/retrieve-events-since store stream-id (:cursor (meta (last (store/retrieve-events store stream-id)))) 0)
             additional-events)))

    (testing "Long polling"
      (let [cursor (:cursor (meta (last (store/retrieve-events store stream-id))))
            post (async/thread
                   (Thread/sleep 2)
                   (store/append-events store stream-id cursor polling-events))
            poll (async/thread
                   (store/retrieve-events-since store stream-id cursor 4))]
        (is (<!! post))
        (is (<!! poll) polling-events)))

    (testing "rough throughput, with overhead"
      (let [cursor (:cursor (meta (last (store/retrieve-events store stream-id))))
            read-buffer-size 10
            write-chunk-size 10
            num-messages 10000
            throughput-events (repeatedly num-messages gen-event)
            recieve (event-channel store stream-id cursor 10)]
        (println "Writing and recieving" num-messages "messages")
        (time
         (let [post (async/thread
                      (reduce (fn [version chunk]
                                (store/append-events store stream-id version chunk)
                                (+ version (count chunk)))
                              cursor
                              (partition-all write-chunk-size throughput-events)))]
           (doseq [e throughput-events]
             (is (= (<!! recieve) e)))
           (async/close! recieve)
           (is (= (+ num-messages cursor) (<!! post)))))))))

