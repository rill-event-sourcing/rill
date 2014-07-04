(ns studyflow.login.prepare-database
  (:require [clojure.java.jdbc :as sql]
            [studyflow.login.main :as login]
            [crypto.password.bcrypt :as bcrypt]))

(defn encrypt [password]
  (bcrypt/encrypt password))

(defn create-user [db role email password]
  (sql/insert! db :users [:uuid :role :email :password]
               [(str (java.util.UUID/randomUUID)) role email (encrypt password)]))

(defn seed-table [db]
  (create-user db "student", "student@studyflow.nl" "student")
  (create-user db "coach", "coach@studyflow.nl" "coach")
  (create-user db "editor", "editor@studyflow.nl" "editor")
  (create-user db "tester", "tester@studyflow.nl" "tester"))

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
