(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [clj-time.local :as time]
            [clj-time.format :as format-time]
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

(defn- local-time []
  (format-time/unparse (format-time/formatter-local "dd-MM-yyyy HH:mm:ss") (time/local-now)))

(defn- local-date []
  (time/format-local-time (time/local-now) :date))

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
        [:a {:href (str "/reports/export?class-id=" (:class-id params)) :target "_blank"} "Exporteren naar Excel"]]))))

(defn sheet-content-for-criterion [criterion class students domains]
  (let [meta-data ["Klas" (:class-name class)
                   "Meijerink criteria geselecteerd" criterion
                   "Date" (local-time)]
        header (into ["Name" "Totaal"] (vec domains))
        student-data (map (fn [student]
                            (into [(h (:full-name student))]
                                  (map (fn [domain]
                                         (completion-export (get-in student [:completion criterion domain])))
                                       (into [:all] domains))))
                          (sort-by :full-name students))
        class-data (into ["Klassengemiddelde"]
                         (map (fn [domain]
                                (completion-export (get-in class [:completion criterion domain])))
                              (into [:all] domains)))]
    (-> [meta-data]
        (conj header)
        (into student-data)
        (conj class-data))))

(defn decorate-workbook [workbook]
  (let [header-style (excel/create-cell-style! workbook {:background :grey_25_percent
                                                         :font {:bold true}})
        footer-style (excel/create-cell-style! workbook {:background :light_cornflower_blue
                                                         :font {:bold true}})]
    (reduce (fn [wb sheet]
              (excel/set-row-style! (second (excel/row-seq sheet))
                                    header-style)
              (excel/set-row-style! (last (excel/row-seq sheet))
                                    footer-style))
            workbook
            (excel/sheet-seq workbook))))

(defn decorate-sheet [sheet-title workbook]
(let [sheet (excel/select-sheet sheet-title workbook)]
        (excel/set-row-style! (second (excel/row-seq sheet))
                              (excel/create-cell-style! workbook {:background :grey_25_percent
                                                                  :font {:bold true}}))
        (excel/set-row-style! (last (excel/row-seq sheet))
                              (excel/create-cell-style! workbook {:background :light_cornflower_blue
                                                                  :font {:bold true}})))
  )

(defn render-export [classes domains students meijerink-criteria params]
  (let [class (first (filter #(= (:class-id params) (:id %)) classes))
        file-name (str "studyflow-export-" (:class-name class) "-" (local-date))
        workbook (reduce (fn [wb criterion]
                           (let [sheet (excel/add-sheet! wb criterion)
                                 data (sheet-content-for-criterion criterion
                                                                   class
                                                                   students
                                                                   domains)]
                             (excel/add-rows! sheet data)
                             wb))
                         (excel/create-workbook (first meijerink-criteria)
                                                (sheet-content-for-criterion (first meijerink-criteria)
                                                                             class
                                                                             students
                                                                             domains))
                         (rest meijerink-criteria))

        sheet (excel/select-sheet (first meijerink-criteria) workbook)]
    (doseq [criterion meijerink-criteria]
      (decorate-sheet criterion workbook))
    {:status 200
     :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
               "Content-Disposition" (str "attachment; filename=\"" file-name "\".xslx" )
               }
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
             domains (sort (read-model/domains read-model))
             meijerink-criteria (sort (read-model/meijerink-criteria read-model))
             class (first (filter #(= class-id (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))]
         (render-export classes domains students meijerink-criteria params))))
