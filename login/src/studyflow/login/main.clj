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


(def redis  {:pool {} :spec {}})

(def default-redirect-path {"editor" publishing-url
                            "tester" "https://staging.studyflow.nl"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(defn layout [title & body]
  (html5
    [:head
      [:title (str/join " - " [app-title title])]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      (include-css "http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/css/bootstrap.css")
      (include-css "screen.css")
      "<!-- [if lt IE 9>]"
      [:script {:src "http://cdnjs.cloudflare.com/ajax/libs/html5shiv/3.7/html5shiv.js"}]
      [:script {:src "http://cdnjs.cloudflare.com/ajax/libs/respond.js/1.3.0/respond.js"}]
      "<! [endif]-->"]
    [:body
      [:div {:class "container"} body]
      "<!-- /container -->"
      [:script {:src "http://cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js"}]
      [:script {:src "http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/js/bootstrap.min.js"}]]))

(defn render-login [email password msg]
  (form/form-to
    {:role "form" :class "form-signin" } [:post "/"]
    (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
    [:h2 {:class "form-signin-heading"} msg]
    (form/email-field {:class "form-control" :placeholder "Email address"} "email" email) ;; required autofocus
    (form/password-field {:class "form-control" :placeholder "Password"} "password" password) ;; required
    [:button {:class "btn btn-lg btn-primary btn-block" :type "submit"} "Sign in"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defn redirect-to [path]
  {:status 302
   :headers {"Location" path}})

(defroutes actions
  (GET "/" {:keys [user-role params]}
       (if user-role
         {:redirect-for-role user-role}
         (layout "Studyflow Beta" (render-login (:email params) (:password params) "Please sign in"))))

  (POST "/" {authenticate :authenticate {:keys [email password]} :params}
        (if-let [user (authenticate email password)]
          (assoc (redirect-to "/") :login-user user)
          (layout "Studyflow Beta" (render-login email password "Wrong email / password combination"))))

  (POST "/logout" {}
       (assoc (redirect-to "/") :logout-user true))

  ;; temporary until POST logout works without anti-forgery-token
  (GET "/logout" {}
       (assoc (redirect-to "/") :logout-user true))
  ;; /temporary until POST logout works without anti-forgery-token

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
  (make-uuid-cookie "" -1))


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
   :subname (env :db-subname)
   :user (env :db-user)
   :password (env :db-password)})


(defn set-studyflow-site-defaults []
  (-> site-defaults
      (assoc-in [:session :cookie-name] "studyflow_login_session")))

(def app
  (->
   actions
   wrap-logout-user
   wrap-login-user
   wrap-logout-user
   wrap-redirect-for-role
   wrap-user-role
   (wrap-defaults (set-studyflow-site-defaults))
   (wrap-authenticator db)))
