(ns rill.event-store.atom-store-test
  (:require [rill.event-store.atom-store-test.local :refer [with-local-atom-store]]
            [rill.event-store :as store]
            [rill.message :refer [defevent]]
            [rill.event-stream :refer [empty-stream-version]]
            [rill.uuid :refer [new-id]]
            [clojure.test :refer [deftest testing is]]))

(defevent TestAtomEvent [stream-id])
(def stream-id (new-id))

(defn gen-event
  []
  (->TestAtomEvent (new-id) stream-id))

(is (= (rill.message/strict-map->Message "TestAtomEvent" {:id "1" :stream-id"2"})
       (->TestAtomEvent "1" "2")))

(def events (repeatedly 2 gen-event))
(def additional-events (repeatedly 2 gen-event))

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
             additional-events)))))



