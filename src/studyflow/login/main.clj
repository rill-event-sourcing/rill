(ns studyflow.login.main
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [compojure.core :refer [DELETE GET POST defroutes]]
            [compojure.route :refer [not-found]]
            [hiccup.form :as form]
            [studyflow.login.credentials :refer [caught-up?]]
            [hiccup.page :refer [html5 include-css include-js]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :refer [content-type]]
            [rill.handler :refer [try-command]]
            [studyflow.login.edu-route-service :refer [get-student-info check-edu-route-signature]]
            [studyflow.login.edu-route-student :as edu-route-student]
            [clojure.tools.logging :as log]))

(def app-title "Studyflow")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(defn layout [title & body]
  (-> {:status 200
       :body (html5
              [:head
               [:title (str/join " - " [app-title title])]
               [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
               (include-css "css/login.css")
               (include-css "//cloud.typography.com/6865512/722124/css/fonts.css")
               (include-js "js/fastclick.js")
               (include-js "js/fastclick-start.js")
               (include-js "js/usersnap.js")
               [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
               "<!-- [if lt IE 9>]"
               [:script {:src "//cdnjs.cloudflare.com/ajax/libs/html5shiv/3.7/html5shiv.js"}]
               [:script {:src "//cdnjs.cloudflare.com/ajax/libs/respond.js/1.3.0/respond.js"}]
               "<! [endif]-->"]
              [:body
               [:div#login_page body]])}
      (content-type "text/html")))

(defn render-login [email password msg]
  (form/form-to
   {:role "form" :id "login_screen" } [:post "/"]
   ;;(form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
   [:h2.login_heading msg]
   [:img {:src "logo.svg" :id "logo_small"}]
   (form/email-field {:class "login_form" :placeholder "E-mailadres"} "email" email)
   (form/password-field {:class "login_form" :placeholder "Wachtwoord"} "password" password)
   [:button.btn.big.yellow.login_button {:type "submit"} "Inloggen"]))

(defn please-wait
  [refresh-count]
  [:h1 "Even geduld..." (str refresh-count)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defn redirect-to [path]
  {:status 303
   :headers {"Location" path}})

(defn refresh [r path seconds]
  (assoc-in r [:headers "Refresh"] (str seconds "; url=" path)))

(defn please-wait-response
  [session refresh-count]
  (-> (layout "Studyflow" (please-wait refresh-count))
      (assoc :session (assoc session :refresh-count  (inc refresh-count)))
      (refresh "/students/sign_in_wait" (* 2 refresh-count))))


(defroutes actions
  (fn [{:keys [credentials]}]
    (when-not (caught-up? credentials)
      {:status 503
       :body "Server starting up."
       :headers {"Content-Type" "text/plain"}}))

  (GET "/" {:keys [user-role params]}
       (if user-role
         {:redirect-for-role user-role}
         (layout "Studyflow" (render-login (:email params) (:password params) ""))))

  (POST "/" {authenticate :authenticate-by-email-and-password {:keys [email password]} :params}
        (if-let [user (authenticate email password)]
          (assoc (redirect-to "/") :login-user user)
          (layout "Studyflow" (render-login email password "Inloggen mislukt"))))

  (GET "/students/sign_in"
       {{:keys [edurouteSessieID signature EAN] :as params} :params
        :keys [session event-store edu-route-service authenticate-by-edu-route-id]}
       (log/debug "eduroute login with params: " params)
       ;; check if eduroute session has a valid format
       (if (check-edu-route-signature edu-route-service edurouteSessieID signature)
         ;; check if eduroute session is valid
         (if-let [{:keys [edu-route-id full-name brin-code] :as edu-route-info} (get-student-info edu-route-service edurouteSessieID)]
           ;; check if we have a registered student with the given edu route id
           (if-let [user (authenticate-by-edu-route-id edu-route-id)]
             ;; succes! happy flow 1: user is an existing student :-)
             (assoc (redirect-to "/") :login-user user)
             ;; happy flow 2: user is new and should get a student account
             ;; fire registration event; school-administration system will
             ;; create new student for us (eventually)
             (do (try-command event-store (edu-route-student/register! edu-route-id full-name brin-code))
                 ;; wait for student to be created
                 ;; redirects to sign_in_wait
                 (please-wait-response (assoc session :edu-route-info edu-route-info) 1)))
           ;; something went wrong while validating the eduroute session.
           (-> (layout "Eduroute authenticatie mislukt" "Eduroute authenticatie mislukt")
               (assoc :status 400)))
         (redirect-to "/")))

  (GET "/students/sign_in_wait"
       {{{:keys [edu-route-id]} :edu-route-info
         refresh-count :refresh-count :as session} :session
         authenticate-by-edu-route-id :authenticate-by-edu-route-id}
       (if (and edu-route-id refresh-count)
         (if-let [user (authenticate-by-edu-route-id edu-route-id)]
           (assoc (redirect-to "/") :login-user user)
           (if (< refresh-count 10)
             (please-wait-response session refresh-count)
             (layout "Studyflow" [:p "Helaas, het is op dit moment erg druk. Probeer het later nog eens."])))
         (layout "Studyflow" [:p "Je sessie is verdwenen. Inloggen werkt alleen als je cookies aan hebt. Probeer het eens met een andere browser."])))

  (DELETE "/" {}
          (assoc (redirect-to "/") :logout-user true))

  (not-found "De pagina kan niet gevonden worden"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(defn wrap-login-user [app]
  (fn [req]
    (let [resp (app req)]
      (if-let [{:keys [user-role user-id]} (:login-user resp)]
        (update-in resp [:session] assoc :user-id user-id :user-role user-role)
        resp))))

(defn wrap-logout-user [app]
  (fn [req]
    (let [resp (app req)]
      (if (:logout-user resp)
        (assoc resp :session nil)
        resp))))

(defn wrap-user-role [app]
  (fn [{{:keys [user-role]} :session :as req}]
    (app (assoc req :user-role user-role))))

(defn wrap-redirect-for-role [app]
  (fn [{:keys [default-redirect-paths cookies] :as req}]
    (let [resp (app req)]
      (if-let [user-role (:redirect-for-role resp)]
        (redirect-to (default-redirect-paths user-role))
        resp))))

(def app
  (-> (var actions)
      wrap-logout-user
      wrap-login-user
      wrap-redirect-for-role
      wrap-user-role))
