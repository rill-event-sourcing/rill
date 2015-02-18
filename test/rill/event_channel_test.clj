(ns rill.event-channel-test
  (:require [rill.event-channel :refer [event-channel]]
            [clojure.test :refer [is deftest testing]]
            [rill.temp-store :refer [given]]
            [rill.event-store :as store]
            [rill.event-stream :refer [any-stream-version empty-stream-version]]
            [rill.message :as message]
            [clojure.core.async :as async :refer [<! <!! go close!]]
            [rill.uuid :refer [new-id]]
            [schema.core :as s]))

(def stream-id (new-id))

(message/defevent TestEvent
  :stream-id s/Uuid
  :val s/Str)

(def events (map-indexed (fn [idx content]
                           (assoc (test-event stream-id content)
                                  message/number idx
                                  message/cursor idx))
                         (apply concat (repeat 5 [:a :b :c :d :e :f]))))

(deftest event-channel-test
  (let [store (given [])]
    (let [channel (event-channel store stream-id empty-stream-version 10)]
      (async/thread
        (doseq [chunk (partition-all 10 events)]
          (is (store/append-events store stream-id any-stream-version chunk))
          (Thread/sleep 50)))
      (is (= (<!! (go
                    (doseq [e events]
                      (let [e' (loop [e' (<! channel)]
                                 (if (= :rill.event-channel/CaughtUp (message/type e'))
                                   (recur (<! channel))
                                   e'))]
                        (is (= e' e))))
                    (close! channel)
                    :done))
             :done)))))
