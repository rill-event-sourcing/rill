(ns studyflow.school-administration.prepare-database
  (:require [clojure.java.jdbc :as sql])
  (:require [studyflow.school-administration.database :refer [db]]))

(defn create-student [db name]
  (sql/insert! db :students [:uuid :name]
               [(str (java.util.UUID/randomUUID)) name]))

(defn seed-table [db]
  (create-student db "student")
  (create-student db "coach")
  (create-student db "editor")
  (create-student db "tester"))

(defn create-table [db]
  (sql/execute! db ["CREATE TABLE IF NOT EXISTS students (uuid VARCHAR(36) PRIMARY KEY,
                         name VARCHAR(256) NOT NULL)"]))

(defn clean-table [db]
  (sql/execute! db ["TRUNCATE students"]))


(defn -main []
  (create-table db)
  (clean-table db)
  (seed-table db))
