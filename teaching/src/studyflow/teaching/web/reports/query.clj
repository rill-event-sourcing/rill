(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.html-util :refer [layout]]
            [ring.util.response :refer [redirect-after-post]]
            [dk.ative.docjure.spreadsheet :as excel]
            [ring.util.io :refer [piped-input-stream]]))

(defn- completion-title [{:keys [finished total]}]
  (str finished "/" total))

(defn- completion-percentage [{:keys [finished total]}]
  (str (Math/round (float (/ (* finished 100) total))) "%"))

(defn- completion-html [{:keys [total] :as completion}]
  (if (and completion (> total 0))
    [:span {:title (completion-title completion)}
     (completion-percentage completion)]
    "&mdash;"))

(defn- completion-export [{:keys [total] :as completion}]
  (if (and completion (> total 0))
    (completion-percentage completion)
    "-"))

(defn- classerize [s]
  (-> s
      str
      str/lower-case
      (str/replace #"[^a-z ]" "")
      (str/replace #"\s+" "-")))

(defn render-completion [classes meijerink-criteria domains students params options]
  (let [meijerink-criteria (sort meijerink-criteria)
        domains (sort domains)
        class (first (filter #(= (:class-id params) (:id %)) classes))
        scope (:meijerink params)
        scope (if (str/blank? scope) nil scope)]
    (layout
     (merge {:title (if class
                      (str "Rapport voor \"" (:full-name class) "\"")
                      "Rapport")} options)

     [:form {:method "GET"}
      (form/drop-down {:onchange "this.form.submit()"}
                      "class-id"
                      (into [["-- Kies klas --" ""]]
                            (sort-by first
                                     (map #(vector (:full-name %) (:id %))
                                          classes)))
                      (:class-id params))
      (form/drop-down {:onchange "this.form.submit()"}
                      "meijerink"
                      (into [["-- Kies Niveau --" ""]]
                            meijerink-criteria)
                      (:meijerink params))]

     (when students
       [:div
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
                          (completion-html (get-in student [:completion scope domain]))])
                       (into [:all] domains))])
               (sort-by :full-name students))]
         [:tfoot
          [:th.average "Klassengemiddelde"]
          (map (fn [domain]
                 [:td.average.number {:class (classerize domain)}
                  (completion-html (get-in class [:completion scope domain]))])
               (into [:all] domains))]]
        (when scope
          [:a {:href (str "/reports/export?class-id=" (:class-id params) "&meijerink=" scope) :target "_blank"} "export"])]))))

(defn render-export [classes domains students params]
  (let [domains (sort domains)
        class (first (filter #(= (:class-id params) (:id %)) classes))
        scope (:meijerink params)
        scope (if (str/blank? scope) nil scope)

        title "Resultaten"
        cols (into ["Name" "Totaal"] (vec domains))
        student-data (map (fn [student]
                            (into [(h (:full-name student))]
                                  (map (fn [domain]
                                         (completion-export (get-in student [:completion scope domain])))
                                       (into [:all] domains))))
                          (sort-by :full-name students))
        class-data (into ["Klassengemiddelde"]
                         (map (fn [domain]
                                (completion-export (get-in class [:completion scope domain])))
                              (into [:all] domains)))
        data (-> [cols]
                 (into student-data)
                 (conj class-data))
        workbook (excel/create-workbook title
                                        data)
        sheet (excel/select-sheet title workbook)
        header-row (first (excel/row-seq sheet))
        footer-row (last (excel/row-seq sheet))]
    (excel/set-row-style! header-row
                          (excel/create-cell-style! workbook {:background :grey_25_percent
                                                              :font {:bold true}}))
    (excel/set-row-style! footer-row
                          (excel/create-cell-style! workbook {:background :light_cornflower_blue
                                                              :font {:bold true}}))
    {:status 200
     :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}
     :body (piped-input-stream
            (fn [out]
              (.write workbook out)))}))

(defroutes app
  (GET "/reports/"
       {}
       (redirect-after-post "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [class-id] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             meijerink-criteria (read-model/meijerink-criteria read-model)
             domains (read-model/domains read-model)
             class (first (filter #(= class-id (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))
             options (assoc flash :redirect-urls redirect-urls)]
         (render-completion classes meijerink-criteria domains students params options)))

  (GET "/reports/export"
       {:keys [read-model teacher]
        {:keys [class-id] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             domains (read-model/domains read-model)
             class (first (filter #(= class-id (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))]
         (render-export classes domains students params))))
