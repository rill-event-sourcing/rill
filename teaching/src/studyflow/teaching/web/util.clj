(ns studyflow.teaching.web.util
  (:require [clojure.string :as str]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]))

(def app-title "Studyflow")

(defn completion-percentage [{:keys [finished total]}]
  (str (Math/round (float (/ (* finished 100) total))) "%"))

(defn completion-title [{:keys [finished total]}]
  (str finished "/" total))

(defn completion-html [{:keys [total] :as completion}]
  (if (and completion (> total 0))
    [:span {:title (completion-title completion)}
     (completion-percentage completion)]
    "&mdash;"))

(defn classerize [s]
  (-> s
      str
      str/lower-case
      (str/replace #"[^a-z ]" "")
      (str/replace #"\s+" "-")))

(defn layout [{:keys [title warning message redirect-urls]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (include-css "/screen.css")]
   [:body
    [:header
     (when redirect-urls
       (form/form-to
        {:role "form" :id "logout-form"} [:post (:login redirect-urls)]
        [:input {:type "hidden" :name "_method" :value "DELETE"}]
        [:button {:type "submit"} "Uitloggen"]))
     [:h1 (h title)]]
    [:div.body
     (when warning [:div.warning (h warning)])
     (when message [:div.message (h message)])
     [:div.container body]]
    [:footer]]))
