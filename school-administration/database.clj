(ns studyflow.school-administration.database
  (:require [environ.core :refer [env]]))

(def studyflow-env (keyword (env :studyflow-env)))
(def db-subname (studyflow-env (env :db-subname)))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname db-subname
   :user (env :db-user)
   :password (env :db-password)})

