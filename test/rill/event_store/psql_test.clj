(ns rill.event-store.psql-test
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store.psql-util :as util]
            [rill.event-store.generic-test-base :refer [test-store]]
            [clojure.test :refer :all]))

(def uri util/event-store-uri)

(def store (psql-event-store uri))

(defn get-clean-psql-store! []
  (if-not (util/table-exists? "rill_events")
    (util/load-schema!)
    (util/clear-db!))
  store)

(deftest test-psql-store
  (test-store get-clean-psql-store!))



