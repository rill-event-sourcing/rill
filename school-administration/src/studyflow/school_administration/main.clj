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
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [studyflow.school-administration.read-model :as m]
   [studyflow.school-administration.read-model.event-handler :refer [load-model]]
   [studyflow.school-administration.student.events :refer [fixture]]))



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

(defn render-new-student-form
  [{:keys [full-name]}]
  (form/form-to
   {:role "form"} [:post "/create_student"]
   (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
   (form/text-field {:placeholder "Full name"} "full-name" full-name)
   [:button {:type "submit"} "Add student"]))

(defn render-student-list
  [students]
  (layout
   "Student list"
   [:table
    [:thead
     [:tr
      [:th "id"]
      [:th "name"]]]
    [:tbody
     (map student-row students)]
    (render-new-student-form nil)]))

(defroutes actions
  (GET "/" {:keys [read-model]}
       (render-student-list (m/list-students read-model)))
  (not-found "Nothing here"))

(defn wrap-read-model
  [f model-atom]
  (fn [request]
    (f (assoc request :read-model @model-atom))))

(def app
  (->
   actions
   (wrap-read-model (atom (load-model fixture)))
   (wrap-defaults site-defaults)))

