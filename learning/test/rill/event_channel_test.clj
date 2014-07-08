(ns rill.event-channel-test
  (:require [rill.event-channel :refer [event-channel]]
            [clojure.test :refer [is deftest testing]]
            [rill.event-store.memory :refer [memory-store]]
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
                           (assoc (->TestEvent (new-id) stream-id content)
                             message/number idx)) [:a :b :c :d :e :f]))

(deftest event-channel-test
  (let [store (memory-store)]
    (let [channel (event-channel store stream-id empty-stream-version 0)]
      (is (store/append-events store stream-id empty-stream-version events))
      (is (= (<!! (go
                   (doseq [e events]
                     (is (= (<! channel) e)))
                   (close! channel)
                   :done))
             :done)))))
