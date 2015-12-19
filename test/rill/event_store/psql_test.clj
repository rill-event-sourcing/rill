(ns rill.event-store.psql-test
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store.psql.tools :as tools]
            [rill.event-store.generic-test-base :refer [test-store]]
            [clojure.test :refer :all]))

(def config {:user (System/getenv "RILL_POSTGRES_USER")
             :password (System/getenv "RILL_POSTGRES_PASSWORD")
             :hostname (System/getenv "RILL_POSTGRES_HOST")
             :port (System/getenv "RILL_POSTGRES_PORT")
             :database (System/getenv "RILL_POSTGRES_DB")})

(defn get-clean-psql-store! [config]
  (tools/clear-db! config)
  (psql-event-store (tools/connection config)))

(deftest test-psql-store
  (if (System/getenv "RILL_POSTGRES_DB")
    (do (tools/load-schema! config)
        (test-store #(get-clean-psql-store! config)))
    (println "Skipped tests for rill postgres backend. Set at least RILL_POSTGRES_DB environment variable to configure and enable tests")))



