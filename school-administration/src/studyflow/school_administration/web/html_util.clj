(ns studyflow.school-administration.web.html-util
  (:require [clojure.string :as str]
            [hiccup.core :refer [h]]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.form :refer [hidden-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(def app-title "Studyflow")

(def ^:dynamic *current-nav-uri* nil)

(defn layout [{:keys [title warning]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    (include-css "/screen.css")]
   [:body
    [:header
     [:nav
      [:ul
       (map (fn [[url label]]
              [:li [:a (if (= url *current-nav-uri*) {} {:href url}) label]])
            {"/list-students" "Students"
             "/list-schools" "Schools"})]]
     [:h1 (h title)]]
    [:div.body
     (when warning [:div.warning (h warning)])
     [:div.container body]]
    [:footer]]))

(defn field-errors [messages]
  (if (seq messages)
    [:ul.errors
     (map #(vector :li (h %)) messages)]))

(defn anti-forgery-field []
  (hidden-field "__anti-forgery-token" *anti-forgery-token*))
