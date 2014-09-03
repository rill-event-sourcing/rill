(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.html-util :refer [layout]]
            [ring.util.response :refer [redirect-after-post]]))

(defn- completion-title [{:keys [finished total]}]
  (str finished "/" total))

(defn- completion-percentage [{:keys [finished total]}]
  (str (Math/round (float (/ (* finished 100) total))) "%"))

(defn- completion [{:keys [total] :as completion}]
  (if (and completion (> total 0))
    [:span {:title (completion-title completion)}
     (completion-percentage completion)]
    "&mdash;"))

(defn- classerize [s]
  (-> s
      str
      str/lower-case
      (str/replace #"[^a-z ]" "")
      (str/replace #"\s+" "-")))

(defn render-completion [classes meijerink-criteria domains students params options]
  (let [meijerink-criteria (sort meijerink-criteria)
        domains (sort domains)
        class (first (filter #(= (:classid params) (:id %)) classes))
        scope (:meijerink params)
        scope (if (str/blank? scope) nil scope)]
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
                            meijerink-criteria)
                      (:meijerink params))]

     (when students
       [:table.students
        [:thead
         [:th.full-name]
         [:th.completion.number "Totaal"]
         (map (fn [domain]
                [:th.domain.number (h domain)])
              domains)]
        [:tbody
         (map (fn [student]
                [:tr.student {:id (str "student-" (:id student))}
                 [:td.full-name
                  (h (:full-name student))]
                 (map (fn [domain]
                        [:td.completion.number {:class (classerize domain)}
                         (completion (get-in student [:completion scope domain]))])
                      (into [:all] domains))])
              (sort-by :full-name students))]
        [:tfoot
         [:th.average "Klassengemiddelde"]
         (map (fn [domain]
                [:td.average.number {:class (classerize domain)}
                 (completion (get-in class [:completion scope domain]))])
              (into [:all] domains))]]))))

(defroutes app
  (GET "/reports/"
       {}
       (redirect-after-post "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [classid] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             meijerink-criteria (read-model/meijerink-criteria read-model)
             domains (read-model/domains read-model)
             class (first (filter #(= classid (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))
             options (assoc flash :redirect-urls redirect-urls)]
         (render-completion classes meijerink-criteria domains students params options))))
