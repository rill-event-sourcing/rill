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
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.carmine :as car :refer (wcar)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(def app-title "Studyflow")
(def studyflow-env (keyword (env :studyflow-env)))
(def publishing-url (studyflow-env (env :publishing-url)))
(def cookie-domain (studyflow-env (env :cookie-domain)))
(def session-max-age (studyflow-env (env :session-max-age)))

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

(defn home [email user-list]
  [:div
   [:h3 "welcome " email]
   [:div
    (str (count user-list) " users logged in")]
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

(defn logged-in-users []
  (wcar* (car/keys "*")))

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
  (wcar* (car/set (:uuid user) (:role user)) (car/expire (:uuid user) session-max-age))
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

(defn default-redirect-path [role]
  (case role
    "editor" publishing-url
    "tester" "https://staging.studyflow.nl"
    "/"))

(defn redirect-user [cookie-path role]
  (if cookie-path
    (redirect-to cookie-path) 
    (redirect-to (default-redirect-path role))))

(defn get-redirect-cookie [cookies]
  (:value (cookies "studyflow_redir_to")))

(defn get-login-cookie [uuid]
  (if cookie-domain
    {:studyflow_session {:value uuid :domain cookie-domain :max-age session-max-age}}
    {:studyflow_session {:value uuid :max-age session-max-age}}))

(defn get-authenticated-response [cookies session user]
  (assoc (redirect-user (get-redirect-cookie cookies) (:role user))
               :session (assoc-user session user)
               :cookies (get-login-cookie (:uuid user))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defroutes actions

  (GET "/" {db :db session :session}
    (if (logged-in? session)
        (layout "home" (home (:loggedin session) (logged-in-users)))
        (response/redirect "/login")))

  (GET "/login" {session :session params :params}
    (layout "login" (login (params :msg) (params :email) (params :password) )))

  (POST "/login" {db :db cookies :cookies session :session {:keys [email password]} :params}
    (if-let [user (find-user db email)]
      (if (authenticate user password)
        (get-authenticated-response cookies session user) 
        (layout "login" (login "wrong email / password combination" email password)))
      (layout "login"  (login "wrong email combination" email password))
      ))

  (GET "/logout" {session :session}
    (assoc (redirect-to "/")
           :session (dissoc-user session)
           :cookies {:studyflow_session {:value "" :max-age -1}}))

  (not-found "Nothing here"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn wrap-db [app db]
  (fn [req]
    (app (assoc req :db db))))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname (env :db-subname) 
   :user (env :db-user) 
   :password (env :db-password)})

(defn count-users  [db]
  (:count (first (sql/query db "SELECT COUNT(*) FROM users"))))

(defn set-studyflow-site-defaults []
  (-> site-defaults
    (assoc-in  [:session :cookie-name] "studyflow_login_session")))

(def app
  (->
   (wrap-defaults actions (set-studyflow-site-defaults))
   (wrap-db db)))

