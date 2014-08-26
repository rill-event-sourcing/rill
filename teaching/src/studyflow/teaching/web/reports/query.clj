(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.html-util :refer [layout]]
            [ring.util.response :refer [redirect]]))

(defn render-completion [classes class students options]
  (layout
   (merge {:title (if class
                    (str "Completion for \"" (:full-name class) "\"")
                    "Completion")} options)

   [:form {:method "GET"}
    (form/drop-down {:onchange "this.form.submit()"}
                    "classid"
                    (into [[]]
                          (sort-by first
                                   (map #(vector (:full-name %) (:id %))
                                        classes)))
                    (:id class))]

   [:table.students
    [:thead
     [:th "Name"]]
    [:tbody
     (map (fn [student]
            [:tr
             [:td (h (:full-name student))]])
          students)]]))

(defroutes app
  (GET "/reports/"
       {}
       (redirect "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model flash]
        {:keys [classid]} :params}
       (let [classes (read-model/classes read-model)
             class (first (filter #(= classid (:id %)) classes))
             students (read-model/students-for-class read-model class)]
         (render-completion classes class students flash))))
