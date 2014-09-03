(ns studyflow.teaching.web.html-util
  (:require [clojure.string :as str]
            [hiccup.core :refer [h]]
            [hiccup.page :refer [html5 include-css]]))

(def app-title "Studyflow")

(defn layout [{:keys [title warning message redirect-urls]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    (include-css "/screen.css")]
   [:body
    [:header
     [:h1 (h title)]]
    [:div.body
     (when warning [:div.warning (h warning)])
     (when message [:div.message (h message)])
     [:div.container body]]
    [:footer]]))
