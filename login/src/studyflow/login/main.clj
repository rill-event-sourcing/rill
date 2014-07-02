(ns studyflow.login.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [crypto.password.bcrypt :as bcrypt]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.element :as element]
            [hiccup.form :as form]
            [ring.util.response :as response]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.carmine :as car :refer (wcar)]))

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

(def server1-conn  {:pool {} :spec {}})

(defmacro wcar*  [& body] `(car/wcar server1-conn ~@body))

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
  (first (sql/query db ["SELECT uuid, role, email, password FROM users WHERE email = ?" email])))

(defn authenticate [user password]
  (bcrypt/check password (:password user)))

(defn expire-session-local [session]
  (dissoc session :loggedin :role :uuid))

(defn expire-session-server [session]
  (wcar* (car/del (:uuid session))))

(defn assoc-user [session user]
  (wcar* (car/set (:uuid user) (:role user)) (car/expire (:uuid user) 600))
  (assoc session :uuid (:uuid user) :loggedin (:email user) :role (:role user)))

(defn dissoc-user [session]
  (expire-session-server session) 
  (expire-session-local session))
 
(defn logged-in? [session]
  (if (= (wcar* (car/exists (:uuid session))) 1) 
      true
      (do
        (cond (contains? session :uuid) (expire-session-local session)) 
        false)))

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

(def app
  (->
   (wrap-defaults actions site-defaults)
   (wrap-db db)))

