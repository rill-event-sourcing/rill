(ns studyflow.login.main
  (:require [clojure.string :as str]
            [compojure.core :refer [DELETE GET POST defroutes]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [studyflow.login.credentials :refer :all]
            [taoensso.carmine :as car]))

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
      (include-css "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/css/bootstrap.css")
      (include-css "screen.css")
      "<!-- [if lt IE 9>]"
      [:script {:src "//cdnjs.cloudflare.com/ajax/libs/html5shiv/3.7/html5shiv.js"}]
      [:script {:src "//cdnjs.cloudflare.com/ajax/libs/respond.js/1.3.0/respond.js"}]
      "<! [endif]-->"]
    [:body
      [:div.container body]
      "<!-- /container -->"
      [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js"}]
      [:script {:src "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/js/bootstrap.min.js"}]]))

(defn render-login [email password msg]
  (form/form-to
    {:role "form" :class "form-signin" } [:post "/"]
    (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
    [:h2.form-signin-heading msg]
    (form/email-field {:class "form-control" :placeholder "Email address"} "email" email) ;; required autofocus
    (form/password-field {:class "form-control" :placeholder "Password"} "password" password) ;; required
    [:button.btn.btn-lg.btn-primary.btn-block {:type "submit"} "Sign in"]))


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

  (DELETE "/" {}
       (assoc (redirect-to "/") :logout-user true))

  (not-found "Nothing here"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Session management with redis

(defmacro wcar*  [& body] `(car/wcar redis ~@body))

(defn create-session [uuid role]
  (let [session-uuid (str (java.util.UUID/randomUUID))]
    (wcar* (car/set session-uuid uuid)
           (car/expire session-uuid session-max-age)
           (car/set uuid role)
           (car/expire uuid session-max-age))
    session-uuid))

(defn delete-session! [session-uuid]
  (let [user-uuid (wcar* (car/get session-uuid))]
    (wcar* (car/del session-uuid)
           (car/del user-uuid))))

(defn role-from-session [session-uuid]
  (let [user-uuid (wcar* (car/get session-uuid))]
    (wcar* (car/get user-uuid))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cookie management

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

(defn wrap-login-user [app]
  (fn [req]
    (let [resp (app req)]
      (if-let [user (:login-user resp)]
        (assoc resp :cookies (make-uuid-cookie (create-session (:uuid user) (:role user))))
        resp))))

(defn wrap-logout-user [app]
  (fn [req]
    (let [resp (app req)]
      (if (:logout-user resp)
        (do
          (delete-session! (get-uuid-from-cookies (:cookies req)))
          (assoc resp :cookies (clear-uuid-cookie)))
        resp))))

(defn wrap-user-role [app]
  (fn [req]
    (let [user-role (-> (:cookies req)
                        get-uuid-from-cookies
                        role-from-session)]
      (app (assoc req :user-role user-role)))))

(defn wrap-redirect-for-role [app]
  (fn [req]
    (let [cookies (:cookies req)
          resp (app req)]
      (if-let [user-role (:redirect-for-role resp)]
        (redirect-to (or (:value (cookies "studyflow_redir_to"))
                         (default-redirect-path user-role)))
        resp))))

(defn set-studyflow-site-defaults []
  (-> site-defaults ;; secure-site-defaults
      (assoc-in [:session :cookie-name] "studyflow_login_session")
      (assoc-in [:security :anti-forgery] false)))

(def app
  (->
   actions
   wrap-logout-user
   wrap-login-user
   wrap-redirect-for-role
   wrap-user-role
   (wrap-defaults (set-studyflow-site-defaults))
   (wrap-authenticator db)))
