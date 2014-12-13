(ns rill.event-store.atom-store.event-test
  (:require [rill.event-store.atom-store.event :as event]
            [rill.temp-store :refer [message=]]
            [rill.message :as message :refer [defevent]]
            [rill.uuid :refer [new-id]]
            [schema.core :as s]
            [clojure.test :refer [deftest is]]))

(defevent MySerializableEvent
  :foo s/Uuid)

(def my-event (my-serializable-event (new-id)))

(deftest serializable-events?
  (is (string? (:edn (:data (event/event->entry my-event)))))
  (is (message= (event/entry->event (event/event->entry my-event))
         my-event)))
