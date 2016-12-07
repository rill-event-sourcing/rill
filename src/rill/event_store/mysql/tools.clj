(ns rill.event-store.mysql.tools
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]))

(defn spec
  "clojure.java.jdbc and jdbc.pool.c3p0 compatible connection spec for
  mysql. `database` name is required. Default connects to
  localhost:3306"
  [{:keys [port user password host database] :or {port 3306 host "127.0.0.1"}}]
  {:pre [database host port]}
  (cond-> {:dbtype "mysql"
           :subprotocol "mysql"
           :subname (str "//" host ":" port "/" database)
           :classname "com.mysql.jdbc.Driver"
           :dbname database
           :port port
           :host host}
    user
    (assoc :user user)
    password
    (assoc :password password)))

(defn base-spec
  [config]
  (spec (assoc config :database "mysql")))

(defn database-exists? [config]
  (seq (jdbc/query (base-spec config)
                   ["SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?" (:database config)])))

(defn load-schema! [config]
  (let [s (slurp (io/resource "rill/event_store/mysql/schema.sql"))]
    (when (database-exists? config)
      (jdbc/execute! (base-spec config) [(str "DROP DATABASE " (:database config))]
                     {:transaction? false}))
    (jdbc/execute! (base-spec config) [(str "CREATE DATABASE " (:database config))]
                   {:transaction? false})
    (jdbc/execute! (spec config) [s])))

(defn clear-db!
  [config]
  (jdbc/execute! (spec config) [(str "TRUNCATE rill_events")]))
