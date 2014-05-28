(ns rill.event-store.t-memory
  (:require [rill.event-store :as store]
            [rill.event-store.memory :as memory]
            [rill.event-stream :as stream]
            [midje.sweet :refer :all]))

(facts "about the event store"
       (let [store (memory/memory-store)]
         (fact "non-existing streams are empty streams"
               (store/retrieve-event-stream store "foo") => stream/empty-stream)

         (fact "we can push onto a non-existing stream using the empty stream"
               (store/append-events store "my-stream" stream/empty-stream [:a :b :c :d]) => truthy)

         (fact "we need the current stream to add events to an existing stream"
               (store/append-events store "my-stream" stream/empty-stream [:e :f]) => falsey)

         (let [s (store/retrieve-event-stream store "my-stream")]
           (fact "we retrieve successfully appended events in chronological order"
                 (:events s) => [:a :b :c :d]
                 (store/append-events store "my-stream" s [:e :f]) => truthy
                 (:events (store/retrieve-event-stream store "my-stream")) => [:a :b :c :d :e :f]))

         (let [s (store/retrieve-event-stream store "my-other-stream")]
           (fact "streams are independent"
                 s => stream/empty-stream
                 (store/append-events store "my-other-stream" s [:e :f]) => truthy
                 (:events (store/retrieve-event-stream store "my-other-stream")) => [:e :f]
                 (:events (store/retrieve-event-stream store "my-stream")) => [:a :b :c :d :e :f]))))
