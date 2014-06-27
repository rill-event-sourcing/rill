(ns studyflow.login.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.element :as element]
            [hiccup.form :as form]
            [ring.util.response :as response]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(def app-title "Studyflow")

(defn layout [title & body]
  (html5
   [:head
    [:title (str/join " - " [app-title title])]
    (include-css "screen.css")]
   [:body
    [:h1 title]
    [:div
      (element/link-to "/" "home")
      (element/link-to "/logout" "logout")]
    body]))

(defn home [session user-count user-list]
  [:div
   [:h2 "welcome " (session :loggedin)]
   [:div
    (str user-count " users registered")]
   [:div
    (str/join "<br />" user-list)]])

(defn login [params]
  (form/form-to [:post "/login"]
    (form/hidden-field "__anti-forgery-token" anti-forgery/*anti-forgery-token*)
    [:div
      [:p (params :msg)]
      [:div
        (form/label "email" "email")
        (form/email-field "email" (params :email))]
      [:div
        (form/label "password" "password")
        (form/password-field "password" (params :password))]
      [:div
        (form/submit-button "login")]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Model

(defn count-users [db]
  (:count
   (first
    (sql/query db "SELECT COUNT(*) FROM users"))))

(defn list-users [db]
  (let [extract (fn [user] (str (:role user) " " (:email user) " " (:uuid user)) )]
    (map extract (sql/query db "SELECT * FROM users"))))

(defn create-user [db role email password]
  (sql/insert! db :users [:uuid :role :email :password]  [(str (java.util.UUID/randomUUID)) role email password]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [db email password]
  (first (sql/query db ["SELECT role, email FROM users WHERE email = ? AND password = ?", email, password])))

(defn logged_in? [session]
  (contains? session :loggedin))

(defn persist! [session user]
  (log/debug user)
  (assoc session :loggedin (user :email) :role (user :role)))

(defn unpersist! [session]
  (dissoc session :loggedin nil :role nil))

(defn redirect_to [session path]
  {:status  302
   :headers {"Location" path}
   :session session})

(defn redirect_path [role]
  (case role
    "editor" "http://beta.studyflow.nl"
    "tester" "https://staging.studyflow.nl"
    "/"))

(defn redirect_home [session]
  (redirect_to session (redirect_path (session :role))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defroutes actions

  (GET "/" {db :db session :session}
    (if
      (logged_in? session)
        (layout "home" (home session (count-users db) (list-users db)))
        (response/redirect "/login")))

  (GET "/login" {session :session params :params}
    (layout "login" (login params)))

  (POST "/login" {db :db session :session params :params}
    (let [user (authenticate db (params :email) (params :password))]
      (if
        (seq user)
        (redirect_home (persist! session user))
        (layout "login" (login (assoc params :msg "wrong email / password combination"))))))

  (GET "/logout" {session :session}
    (redirect_to (unpersist! session) "/"))

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

(defn seed-database [db]
  (create-user db "student", "student@studyflow.nl" "student")
  (create-user db "coach", "coach@studyflow.nl" "coach")
  (create-user db "editor", "editor@studyflow.nl" "editor")
  (create-user db "tester", "test@studyflow.nl" "test"))
 
(defn bootstrap! []
  (sql/execute! db [
    (str "CREATE TABLE IF NOT EXISTS users (uuid VARCHAR(36) PRIMARY KEY, "
         "role varchar(16) NOT NULL, "
         "email varchar(255) NOT NULL, "
         "password varchar(255) NOT NULL"
         ")")]))

(def app
  (->
   (wrap-defaults actions site-defaults)
   (wrap-db db)))

