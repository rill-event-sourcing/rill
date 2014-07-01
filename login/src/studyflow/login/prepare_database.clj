(ns studyflow.login.prepare-database
 (:require [clojure.java.jdbc :as sql]
  :require [studyflow.login.main :as login]))

(defn seed-table [db]
  (login/create-user db "student", "student@studyflow.nl" "student")
  (login/create-user db "coach", "coach@studyflow.nl" "coach")
  (login/create-user db "editor", "editor@studyflow.nl" "editor")
  (login/create-user db "tester", "tester@studyflow.nl" "tester"))
 
(defn create-table [db]
  (sql/execute! db [(str "CREATE TABLE IF NOT EXISTS users (uuid VARCHAR(36) PRIMARY KEY, "
         "role VARCHAR(16) NOT NULL, "
         "email VARCHAR(255) NOT NULL, "
         "password VARCHAR(255) NOT NULL"
         ")")]))

(defn clean-table [db]
    (sql/execute! db ["TRUNCATE users"]))

(defn -main []
  (create-table login/db)
  (clean-table login/db)
  (seed-table login/db))
