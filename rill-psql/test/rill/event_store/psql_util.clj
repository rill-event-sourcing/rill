(ns rill.event-store.psql-util
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]))

(def base-uri "postgresql://localhost:5432")
(def ^:dynamic *event-store-uri* (str base-uri "/rill_test"))

(defn database-exists? [db-name]
  (seq (j/query base-uri ["SELECT * FROM pg_database WHERE datname = ?" db-name])))

(defn table-exists? [table-name]
  (and (database-exists? "rill_test")
       (seq (j/query *event-store-uri* ["SELECT * FROM pg_class WHERE relname = ?" table-name]))))

(defn load-schema! []
  (let [s (slurp (io/resource "rill_psql/psql_schema.sql"))]
    (when-not (database-exists? "rill_test")
      (j/execute! base-uri ["CREATE DATABASE rill_test"] :transaction? false))
    (j/execute! *event-store-uri* [s])))

(defn clear-db!
  []
  (j/execute! *event-store-uri* ["TRUNCATE rill_events"]))

