(ns studyflow.login.credentials
  (:require [clojure.java.jdbc :as sql]
            [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [environ.core :refer [env]]))

(def db-subname (-> (keyword (env :studyflow-env))
                    (env :db-subname)))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname db-subname
   :user (env :db-user)
   :password (env :db-password)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Database interaction

(defn- find-user-by-email [db email]
  (first (sql/query db ["SELECT uuid, role, password FROM users WHERE email = ?" email])))

(defn authenticate [db email password]
  (if-let [user (find-user-by-email db email)]
    (if (bcrypt/check password (:password user))
      user)))

(defn wrap-authenticator [app db]
  (fn [req]
    (app (assoc req :authenticate (partial authenticate db)))))
