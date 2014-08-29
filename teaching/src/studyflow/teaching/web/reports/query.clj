(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.html-util :refer [layout]]
            [ring.util.response :refer [redirect]]))

(defn- completion-title [{:keys [finished total]}]
  (str finished "/" total))

(defn- completion-percentage [{:keys [finished total]}]
  (str (Math/round (float (/ (* finished 100) total))) "%"))

(defn- completion [completion]
  (when completion
    [:span {:title (completion-title completion)}
     (completion-percentage completion)]))

(defn render-completion [classes meijerink-criteria students params options]
  (let [class (first (filter #(= (:classid params) (:id %)) classes))
        scope (:meijerink params)
        scope (if (str/blank? scope) :total scope)]
    (layout
     (merge {:title (if class
                      (str "Completion for \"" (:full-name class) "\"")
                      "Completion")} options)

     [:form {:method "GET"}
      (form/drop-down {:onchange "this.form.submit()"}
                      "classid"
                      (into [["-- Kies klas --" ""]]
                            (sort-by first
                                     (map #(vector (:full-name %) (:id %))
                                          classes)))
                      (:classid params))
      (form/drop-down {:onchange "this.form.submit()"}
                      "meijerink"
                      (into [["-- Kies Meijerink --" ""]]
                            (sort meijerink-criteria))
                      (:meijerink params))]

     (when students
       [:table.students
        [:thead
         [:th.full-name]
         [:th.completion (if (= :total scope) "Totaal" scope)]]
        [:tbody
         (map (fn [student]
                [:tr
                 [:td.full-name
                  (h (:full-name student))]
                 [:td.completion
                  (completion (get-in student [:completion scope]))]])
              (sort-by :full-name students))]
        [:tfoot
         [:th.average "Klassengemiddelde"]
         [:td.average-completion
          (completion (get-in class [:completion scope]))]]]))))

(defroutes app
  (GET "/reports/"
       {}
       (redirect "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model flash teacher]
        {:keys [classid] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             meijerink-criteria (read-model/meijerink-criteria read-model)
             class (first (filter #(= classid (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))]
         (render-completion classes meijerink-criteria students params flash))))
