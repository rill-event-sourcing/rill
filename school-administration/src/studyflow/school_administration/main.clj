(ns studyflow.school-administration.main
  (:require
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :refer [not-found]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.element :as element]
            [hiccup.form :as form]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View


(def app-title "Studyflow")

(defn student-row
  [{:keys [id full-name]}]
  [:tr
   [:td id]
   [:td full-name]])

(defn layout [title & body]
  (html5
    [:head
      [:title (str/join " - " [app-title title])]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     (include-css ""//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/css/bootstrap.css)
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

(defn render-student-list
  [students]
  [:table
   [:thead
    [:tr
      [:th "id"]
      [:th "name"]]]
   [:tbody
     (map student-row students)]])

(defroutes actions
  (GET "/" _
    (render-student-list []))

  (not-found "Nothing here"))

(def app
  (->
   actions
   (wrap-defaults site-defaults)))
