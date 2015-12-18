(ns rill.event-store.memory-test
  (:require [rill.event-store.memory :as memory]
            [rill.event-store.generic-test-base :refer [test-store]]
            [clojure.test :refer [deftest]]))

(deftest in-memory-event-store
  (test-store memory/memory-store))

