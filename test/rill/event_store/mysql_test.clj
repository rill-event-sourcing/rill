(ns rill.event-store.mysql-test
  (:require [rill.event-store.mysql :refer [mysql-event-store]]
            [rill.event-store.mysql.tools :as tools]
            [jdbc.pool.c3p0 :as pool]
            [rill.event-store.generic-test-base :refer [test-store]]
            [clojure.test :refer :all]))

(def config {:user     (System/getenv "RILL_MYSQL_USER")
             :password (System/getenv "RILL_MYSQL_PASSWORD")
             :hostname (System/getenv "RILL_MYSQL_HOST")
             :port     (System/getenv "RILL_MYSQL_PORT")
             :database (System/getenv "RILL_MYSQL_DB")})

(defn get-clean-mysql-store! [config]
  (tools/clear-db! config)
  (mysql-event-store (pool/make-datasource-spec (tools/spec config))))

(deftest test-mysql-store
  (if (System/getenv "RILL_MYSQL_DB")
    (do (tools/load-schema! config)
        (test-store #(get-clean-mysql-store! config)))
    (println "Skipped tests for rill mysql backend. Set at least RILL_MYSQL_DB environment variable to configure and enable tests")))
