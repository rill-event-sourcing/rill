(ns studyflow.login.main
  (:require [clojure.string :as str]
            [compojure.core :refer [DELETE GET POST defroutes]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [studyflow.components.session-store :refer [create-session delete-session! get-user-id get-role]]
            [taoensso.carmine :as car]))

(def app-title "Studyflow")
(assert (env :studyflow-env) "login requires .lein-env on path")
(def studyflow-env (keyword (env :studyflow-env)))
(def publishing-url (studyflow-env (env :publishing-url)))
(def learning-url (studyflow-env (env :learning-url)))
(def cookie-domain (studyflow-env (env :cookie-domain)))
(def session-max-age (studyflow-env (env :session-max-age)))

(def default-redirect-path {"editor" publishing-url
                            "student" learning-url
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
;; Cookie management

(defn get-session-id-from-cookies [cookies]
  (:value (get cookies "studyflow_session")))

(defn make-session-cookie [session-id & [max-age]]
  (let [max-age (or max-age session-max-age)]
    (if cookie-domain
      {:studyflow_session {:value session-id :max-age max-age :domain cookie-domain}}
      {:studyflow_session {:value session-id :max-age max-age}})))

(defn clear-session-cookie []
  (make-session-cookie "" -1))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn wrap-login-user [app]
  (fn [{:keys [session-store] :as req}]
    (let [resp (app req)]
      (if-let [user (:login-user resp)]
        (assoc resp :cookies (make-session-cookie (create-session session-store (:uuid user) (:role user) session-max-age)))
        resp))))

(defn wrap-logout-user [app]
  (fn [{:keys [session-store] :as req}]
    (let [resp (app req)]
      (if (:logout-user resp)
        (do
          (delete-session! session-store (get-session-id-from-cookies (:cookies req)))
          (assoc resp :cookies (clear-session-cookie)))
        resp))))

(defn wrap-user-role [app]
  (fn [{:keys [session-store] :as req}]
    (let [user-role (get-role session-store (get-session-id-from-cookies (:cookies req)))]
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
   (wrap-defaults (set-studyflow-site-defaults))))
