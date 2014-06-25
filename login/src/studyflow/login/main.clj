(ns studyflow.login.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5 include-css]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(def app-title "Studyflow login")

(defn layout [title & body]
  (html5
   [:head
    [:title (str/join "-" [app-title title])]
    (include-css "screen.css")]
   [:body
    [:h1 title]
    body]))

(defn home [user-count user-list]
  [:div
   [:h2 "welcome home"]
   [:div
    (str user-count " users registered")]
   [:div
    (str/join " ," user-list)]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Model

(defn count-users [db]
  (:count
   (first
    (sql/query db "SELECT COUNT(*) FROM users"))))

(defn list-users [db]
  (map :uuid  (sql/query db "SELECT * FROM users")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defroutes actions
  (GET "/" {db :db}
       (layout "HOME" (home (count-users db) (list-users db))))
  (not-found "Nothing here"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring


(defn wrap-db [app db]
  (fn [req]
    (app (assoc req :db db))))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname (or (env :db-subname) "//localhost/studyflow_login")
   :user (or (env :db-user) "studyflow")
   :password (or (env :db-password) "studyflow")})

(defn bootstrap! []
  (sql/execute! db ["CREATE TABLE IF NOT EXISTS users (uuid VARCHAR(36) PRIMARY KEY)"]))

(def app
  (->
   (wrap-defaults actions site-defaults)
   (wrap-db db)))

