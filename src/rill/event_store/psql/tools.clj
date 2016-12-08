(ns rill.event-store.psql.tools
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]))

(defn connection
  [{:keys [port user password host database]}]
  (cond-> {:subprotocol "postgresql"
           :classname "org.postgresql.Driver"
           :subname (format "//%s:%s/%s"
                            (or host "localhost")
                            (or port "5432") database)}
    user
    (assoc :user user)
    password
    (assoc :password password)))

(defn base-connection
  [config]
  (connection (assoc config :database "postgres")))

(defn database-exists? [config]
  (seq (jdbc/query (base-connection config)
                   ["SELECT * FROM pg_database WHERE datname = ?" (:database config)])))
(defn load-schema! [config]
  (let [s (slurp (io/resource "rill/event_store/psql/psql_schema.sql"))]
    (when (database-exists? config)
      (jdbc/execute! (base-connection config) [(str "DROP DATABASE " (:database config))]
                     {:transaction? false}))
    (jdbc/execute! (base-connection config) [(str "CREATE DATABASE " (:database config))]
                   {:transaction? false})
    (jdbc/execute! (connection config) [s])))

(defn clear-db!
  [config]
  (jdbc/execute! (connection config) [(str "TRUNCATE rill_events")]))
