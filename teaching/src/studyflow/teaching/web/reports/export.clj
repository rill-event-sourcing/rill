(ns studyflow.teaching.web.reports.export
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clj-time.local :as time]
            [clj-time.format :as format-time]
            [studyflow.teaching.web.util :refer [completion-percentage time-spent-str]]
            [dk.ative.docjure.spreadsheet :as excel]
            [ring.util.io :refer [piped-input-stream]]))

(defn- local-time []
  (format-time/unparse (format-time/formatter-local "dd-MM-yyyy HH:mm:ss") (time/local-now)))

(defn- local-date []
  (time/format-local-time (time/local-now) :date))

(defn- completion-export [{:keys [finished total] :as completion}]
  (if (and completion (> total 0))
    [finished (completion-percentage completion)]
    ["-" "-"]))

(defn sheet-content-for-criterion [criterion class students domains]
  (let [meta-data ["Studyflow Rapport"
                   (str  "Klas: " (:class-name class))
                   (str  "Meijerink: " criterion)
                   (str  "Datum: " (local-time))]
        sup-header (into ["" "Tijd"]
                         (interleave (into ["Totaal"] domains)
                                     (repeat "")))
        domains-all (into [:all]
                          domains)
        domains-total (apply hash-map
                             (interleave domains-all
                                         (map (fn [x] (:total  (get-in (first students) [:completion criterion x])))
                                              domains-all)))
        header (reduce into
                       ["Leerling naam"
                        "Tijd"]
                       (map (fn [x]
                              [(str "# Hoofdstukken afgerond (totaal " (get domains-total x) ")")
                               "Percentage afgerond"])
                            domains-all))
        student-data (map (fn [student] (reduce into [(:full-name student)
                                                      (time-spent-str (get-in student [:time-spent criterion]))]
                                                (map (fn [domain]
                                                       (completion-export (get-in student [:completion criterion domain])))
                                                     domains-all)))
                          (sort-by :full-name students))
        class-data (reduce into ["Klassengemiddelde"
                                 (time-spent-str (get-in class [:time-spent criterion]))]
                           (map (fn [domain]
                                  (completion-export (get-in class [:completion criterion domain])))
                                (into [:all] domains)))]
    (-> [meta-data]
        (conj sup-header)
        (conj header)
        (into student-data)
        (conj class-data))))



(defn decorate-sheet [sheet-title workbook column-numbers]
  (let [sheet (excel/select-sheet sheet-title workbook)]
    (.setColumnWidth sheet 0 8000)
    (doseq [col (range 0 column-numbers)]
      (.setColumnWidth sheet
                       (inc col)
                       6000))
    (excel/set-cell-style! (first (excel/cell-seq (first (excel/row-seq sheet))))
                           (excel/create-cell-style! workbook {:font {:bold true}}))
    (excel/set-row-style! (second (excel/row-seq sheet))
                          (excel/create-cell-style! workbook {:background :grey_25_percent
                                                              :font {:bold true}}))
    (excel/set-row-style! (last (excel/row-seq sheet))
                          (excel/create-cell-style! workbook {:background :light_cornflower_blue
                                                              :font {:bold true}}))))

(defn render-export [class students domains meijerink-criteria]
  (let [file-name (str "studyflow-rapport-" (:class-name class) "-" (local-date))
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
      (decorate-sheet criterion workbook (* 2 (inc (count domains)))))
    {:status 200
     :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
               "Content-Disposition" (str "attachment; filename=\"" file-name ".xlsx\"")}
     :body (piped-input-stream
            (fn [out]
              (.write workbook out)))}))
