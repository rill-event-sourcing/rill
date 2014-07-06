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

(def app-title "Studyflow")
(def studyflow-env (keyword (env :studyflow-env)))
(def publishing-url (studyflow-env (env :publishing-url)))
(def cookie-domain (studyflow-env (env :cookie-domain)))
(def session-max-age (studyflow-env (env :session-max-age)))
(def db-subname (studyflow-env (env :db-subname)))


(def redis  {:pool {} :spec {}})

(def default-redirect-path {"editor" publishing-url
                            "tester" "https://staging.studyflow.nl"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

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

(defn render-login [email password & [msg]]
  (form/form-to
   {:class "login" } [:post "/"]
   (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
   [:div
    (if msg [:p.warning msg])
    [:div
     (form/label "email" "email")
     (form/email-field "email" email)]
    [:div
     (form/label "password" "password")
     (form/password-field "password" password)]
    [:div
     (form/submit-button "login")]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defn redirect-to [path]
  {:status 302
   :headers {"Location" path}})

(defroutes actions
  (GET "/" {:keys [user-role params]}
       (if user-role
         {:redirect-for-role user-role}
         (layout "login" (render-login (:email params) (:password params)))))

  (POST "/" {authenticate :authenticate {:keys [email password]} :params}
        (if-let [user (authenticate email password)]
          (assoc (redirect-to "/") :login-user user)
          (layout "login" (render-login email password "wrong email / password combination"))))

  (POST "/logout" {}
       (assoc (redirect-to "/") :logout-user true))

  (not-found "Nothing here"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Model

(defmacro wcar*  [& body] `(car/wcar redis ~@body))

(defn deregister-uuid! [uuid]
  (wcar* (car/del uuid)))

(defn register-uuid! [uuid role]
  (wcar* (car/set uuid role)
         (car/expire uuid session-max-age)))

(defn role-for-uuid [uuid]
  (wcar* (car/get uuid)))


;;;;;;;;;;;;

(defn- find-user-by-email [db email]
  (first (sql/query db ["SELECT uuid, role, password FROM users WHERE email = ?" email])))

(defn authenticate [db email password]
  (if-let [user (find-user-by-email db email)]
    (if (bcrypt/check password (:password user))
      user)))

;;;;;;;;;;;;;;;;;;

(defn get-uuid-from-cookies [cookies]
  (:value (get cookies "studyflow_session")))

(defn make-uuid-cookie [uuid & [max-age]]
  (let [max-age (or max-age session-max-age)]
    (if cookie-domain
      {:studyflow_session {:value uuid :max-age max-age :domain cookie-domain}}
      {:studyflow_session {:value uuid :max-age max-age}})))

(defn clear-uuid-cookie []
  (make-uuid-cookie nil -1))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn wrap-authenticator [app db]
  (fn [req]
    (app (assoc req :authenticate (partial authenticate db)))))

(defn wrap-login-user [app]
  (fn [req]
    (let [resp (app req)]
      (if-let [user (:login-user resp)]
        (do
          (register-uuid! (:uuid user) (:role user))
          (assoc resp :cookies (make-uuid-cookie (:uuid user))))
        resp))))

(defn wrap-logout-user [app]
  (fn [req]
    (let [resp (app req)]
      (if (:logout-user resp)
        (do
          (deregister-uuid! (get-uuid-from-cookies (:cookies req)))
          (assoc resp :cookies (clear-uuid-cookie)))
        resp))))

(defn wrap-user-role [app]
  (fn [req]
    (let [user-role (role-for-uuid (get-uuid-from-cookies (:cookies req)))]
      (app (assoc req :user-role user-role)))))

(defn wrap-redirect-for-role [app]
  (fn [req]
    (let [cookies (:cookies req)
          resp (app req)]
      (if-let [user-role (:redirect-for-role resp)]
        (redirect-to (or (:value (cookies "studyflow_redir_to"))
                         (default-redirect-path user-role)))
        resp))))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname db-subname
   :user (env :db-user)
   :password (env :db-password)})


(defn set-studyflow-site-defaults []
  (-> site-defaults
      (assoc-in [:session :cookie-name] "studyflow_login_session")))

(def app
  (->
   actions
   wrap-login-user
   wrap-redirect-for-role
   wrap-user-role
   (wrap-defaults (set-studyflow-site-defaults))
   (wrap-authenticator db)))
