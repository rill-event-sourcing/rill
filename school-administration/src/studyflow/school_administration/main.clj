(ns studyflow.school-administration.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.element :as element]
            [hiccup.form :as form]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))



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

;;(defn render-login [email password msg]
;;  (form/form-to
;;    {:role "form" :class "form-signin" } [:post "/"]
;;    (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
;;    [:h2.form-signin-heading msg]
;;    (form/email-field {:class "form-control" :placeholder "Email address"} "email" email) ;; required autofocus
;;    (form/password-field {:class "form-control" :placeholder "Password"} "password" password) ;; required
;;    [:button.btn.btn-lg.btn-primary.btn-block {:type "submit"} "Sign in"]))
;;

(defroutes actions
  (GET "/" _
    "Studyflow Beta")

  (not-found "Nothing here"))




(def app
  (->
   actions
   (wrap-defaults site-defaults)))

