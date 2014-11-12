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
     [:tbody
      [:tr [:td [:a {:href "#"} "Docent omgeving"]]]
      [:tr [:td [:a {:href "#"} "Leerling omgeving"]]]
      [:tr [:td [:a {:href "#"} "Starttoetsen rekenen"]]]
      [:tr [:td [:a {:href "#"} "Leerlingen"]]]]]]))

(defroutes manuals-routes
  (GET "/handleidingen"
       {:keys [redirect-urls] params :params}
       (let [options {:redirect-urls redirect-urls
                      :title "Handleidingen"}]
         (manuals params options))))
