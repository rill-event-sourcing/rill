(ns rill.event-channel-test
  (:require [rill.event-channel :refer [event-channel]]
            [clojure.test :refer [is deftest testing]]
            [rill.temp-store :refer [given]]
            [rill.event-store :as store]
            [rill.event-stream :refer [empty-stream-version]]
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
                                  message/cursor idx)) [:a :b :c :d :e :f]))

(deftest event-channel-test
  (let [store (given [])]
    (let [channel (event-channel store stream-id empty-stream-version 0)]
      (is (store/append-events store stream-id empty-stream-version events))
      (is (= (<!! (go
                    (doseq [e events]
                      (is (= (<! channel) e)))
                    (close! channel)
                    :done))
             :done)))))
