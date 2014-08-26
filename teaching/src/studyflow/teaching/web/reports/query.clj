(ns studyflow.teaching.web.reports.query
  (:require [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.html-util :refer [layout]]
            [ring.util.response :refer [redirect]]))

(defn render-completion [_ options]
  (layout
   (merge {:title "Completion"} options)

   [:p (h "TODO")]))

(defroutes app
  (GET "/reports/"
       {}
       (redirect "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model flash]}
       (render-completion nil flash)))
