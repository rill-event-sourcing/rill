(ns rill.event-store.atom-store.event
  (:require [rill.event-store.atom-store.event :as event]
            [rill.message :as message :refer [defevent]]
            [rill.uuid :refer [new-id]]
            [schema.core :as s]
            [clojure.test :refer [deftest is]]))

(defevent MySerializableEvent [foo :- s/Uuid])

(def my-event (->MySerializableEvent (new-id) (new-id)))

(deftest serializable-events?
  (is (string? (:edn (:data (event->entry my-event)))))
  (is (= (entry->event (event->entry my-event))
         my-event)))
