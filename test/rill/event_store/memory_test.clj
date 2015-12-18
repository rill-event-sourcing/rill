(ns rill.event-store.memory-test
  (:require [rill.event-store.memory :as memory]
            [rill.event-store.generic-test-base :refer [basic-examples]]
            [clojure.test :refer [deftest]]))

(deftest in-memory-event-store
  (basic-examples (memory/memory-store)))

