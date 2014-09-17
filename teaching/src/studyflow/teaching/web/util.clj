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

(def ^:dynamic *current-nav-uri* nil)

(defn layout [{:keys [title warning message redirect-urls]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (include-css "/css/teaching.css")]
   [:body
    [:header#m-top_header
     [:h1#logo "Leraren"]
     (when redirect-urls
       (form/form-to
        {:role "form" :id "logout-form"} [:post (:login redirect-urls)]
        [:input {:type "hidden" :name "_method" :value "DELETE"}]
        [:button {:type "submit"} "Uitloggen"]))]
    [:section#main_teaching
     [:nav#main_container_nav
      [:ul
       (map (fn [[url label]]
              [:li.main_container_nav_list_item
               [:a.main_container_nav_tab (if (= url *current-nav-uri*) {:class "selected"} {:href url}) label]])
            [["/reports/completion" "Rapport"]
             ["/reports/chapter-list" "Voortgang"]])]]
     [:div.body
      (when warning [:div.warning (h warning)])
      (when message [:div.message (h message)])
      [:div.container body]]]
    [:footer]]))
