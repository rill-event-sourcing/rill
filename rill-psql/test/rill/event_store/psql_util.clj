(ns rill.event-store.psql-util
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]))

(defn db-config
  [db]
  (cond-> {:subprotocol "postgresql"
           :classname "org.postgresql.Driver"
           :subname (str "//" (or (System/getenv "RILL_POSTGRES_HOST"))
                         "localhost"
                         ":" (or (System/getenv "RILL_POSTGRES_PORT")
                                 "5432")
                         "/" db)}
    (System/getenv "RILL_POSTGRES_USER")
    (assoc :user (System/getenv "RILL_POSTGRES_USER"))
    (System/getenv "RILL_POSTGRES_PASSWORD")
    (assoc :password (System/getenv "RILL_POSTGRES_PASSWORD"))))

(def event-store-db
  "database name that will be used to create the test schema"
  (or (System/getenv "RILL_POSTGRES_DB")
      "rill_test"))

(def base-uri
  "Connection to `system' database that should always exist"
  (db-config "postgres"))

(def event-store-uri
  "Connection to generated schema that will be created by these tests"
  (db-config event-store-db))

(defn database-exists? [db-name]
  (seq (j/query base-uri ["SELECT * FROM pg_database WHERE datname = ?" db-name])))

(defn table-exists? [table-name]
  (and (database-exists? event-store-db)
       (seq (j/query event-store-uri ["SELECT * FROM pg_class WHERE relname = ?" table-name]))))

(defn load-schema! []
  (let [s (slurp (io/resource "rill_psql/psql_schema.sql"))]
    (when-not (database-exists? event-store-db)
      (j/execute! base-uri [(str "CREATE DATABASE " event-store-db)] :transaction? false))
    (j/execute! event-store-uri [s])))

(defn clear-db!
  []
  (j/execute! event-store-uri [(str "TRUNCATE rill_events")]))

