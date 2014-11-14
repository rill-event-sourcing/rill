(ns studyflow.teaching.web.pages.manuals
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [ring.util.codec :refer [url-encode]]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.util :refer :all]
            [rill.uuid :refer [uuid]]))

(defn manuals [params options]
  (layout
   options
   nil
   nil

   [:h1#page-title "Handleidingen"]
   [:div.m-teacher-manuals
    [:table.manuals
     [:thead
      [:th "Downloaden als PDF"]]
     (let [assets-url "https://assets.studyflow.nl/teaching/"]
       [:tbody
        [:tr [:td [:a {:href (str assets-url "studyflow_gebruikershandleiding_docenten.pdf")} "Gebruikershandleiding voor docenten"]]]
        [:tr [:td [:a {:href (str assets-url "studyflow_gebruikershandleiding_leerlingen.pdf")} "Gebruikershandleiding voor leerlingen"]]]
        [:tr [:td [:a {:href (str assets-url "studyflow_gebruikershandleiding_toetsen.pdf")} "Gebruikershandleiding voor de toetsen"]]]
        [:tr [:td [:a {:href (str assets-url "studyflow_weekplanning.pdf")} "Weekplanning voor hoofdstukken en paragrafen"]]]])]]))

(defroutes manuals-routes
  (GET "/handleidingen"
       {:keys [redirect-urls] params :params}
       (let [options {:redirect-urls redirect-urls
                      :title "Handleidingen"}]
         (manuals params options))))
