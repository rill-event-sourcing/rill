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
            [crypto.password.bcrypt :as bcrypt]
            [ring.util.response :as response]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
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
   [:h3 "welcome " (session :loggedin)]
   [:div
    (str user-count " users registered")]
   [:div
    (str/join "<br />" user-list)]])

(defn login [msg email password]
  (form/form-to [:post "/login"]
    (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
    [:div
      [:p msg]
      [:div
        (form/label "email" "email")
        (form/email-field "email" email)]
      [:div
        (form/label "password" "password")
        (form/password-field "password" password)]
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

(defn encrypt [password]
  (bcrypt/encrypt password))

(defn create-user [db role email password]
  (sql/insert! db :users [:uuid :role :email :password]  [(str (java.util.UUID/randomUUID)) role email (encrypt password)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-user [db email]
  (first (sql/query db ["SELECT role, email, password FROM users WHERE email = ?" email])))

(defn authenticate [user password]
  (bcrypt/check password (:password user)))

(defn logged-in? [session]
  (contains? session :loggedin))

(defn assoc-user [session user]
  (log/debug user)
  (assoc session :loggedin (:email user) :role (:role user)))

(defn dissoc-user [session]
  (dissoc session :loggedin :role))

(defn redirect-to [path]
  {:status  302
   :headers {"Location" path}})

(defn redirect-path [role]
  (case role
    "editor" "http://beta.studyflow.nl"
    "tester" "https://staging.studyflow.nl"
    "/"))

(defn redirect-home [role]
  (redirect-to (redirect-path role)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defroutes actions

  (GET "/" {db :db session :session}
    (if (logged-in? session)
        (layout "home" (home session (count-users db) (list-users db)))
        (response/redirect "/login")))

  (GET "/login" {session :session params :params}
    (layout "login" (login (params :msg) (params :email) (params :password) )))

  (POST "/login" {db :db session :session {:keys [email password]} :params}
    (if-let [user (find-user db email)]
      (if (authenticate user password)
        (assoc (redirect-home (:role user))
               :session (assoc-user session user))
        (layout "login" (login "wrong email / password combination" email password)))
      (layout "login"  (login "wrong email combination" email password))
      ))

  (GET "/logout" {session :session}
    (assoc (redirect-to "/")
           :session (dissoc-user session)))

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

(defn empty-database [db]
  (sql/execute! db ["TRUNCATE users;"])) 

(defn seed-database [db]
  (create-user db "student", "student@studyflow.nl" "student")
  (create-user db "coach", "coach@studyflow.nl" "coach")
  (create-user db "editor", "editor@studyflow.nl" "editor")
  (create-user db "tester", "tester@studyflow.nl" "tester"))
 
(defn bootstrap! []
  (sql/execute! db [
    (str "CREATE TABLE IF NOT EXISTS users (uuid VARCHAR(36) PRIMARY KEY, "
         "role VARCHAR(16) NOT NULL, "
         "email VARCHAR(255) NOT NULL, "
         "password VARCHAR(255) NOT NULL"
         ")")]))

(def app
  (->
   (wrap-defaults actions site-defaults)
   (wrap-db db)))

