(ns studyflow.teaching.web.html-util
  (:require [clojure.string :as str]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]))

(def app-title "Studyflow")

(defn layout [{:keys [title warning message redirect-urls]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    (include-css "/screen.css")]
   [:body
    [:header
     [:h1 (h title)]
     (when redirect-urls
       (form/form-to
        {:role "form" :id "logout-form"} [:post (:login redirect-urls)]
        [:input {:type "hidden" :name "_method" :value "DELETE"}]
        [:button {:type "submit"} "Uitloggen"]))]
    [:div.body
     (when warning [:div.warning (h warning)])
     (when message [:div.message (h message)])
     [:div.container body]]
    [:footer]]))
