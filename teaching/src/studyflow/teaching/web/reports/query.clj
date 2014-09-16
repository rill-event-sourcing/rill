(ns studyflow.teaching.web.reports.query
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.reports.export :refer [render-export]]
            [studyflow.teaching.web.util :refer :all]
            [ring.util.response :refer [redirect-after-post]]))

(defn drop-down-classes [classes params]
  (form/drop-down {:onchange "this.form.submit()"}
                      "class-id"
                      (into [["-- Kies klas --" ""]]
                            (sort-by first
                                     (map #(vector (:full-name %) (:id %))
                                          classes)))
                      (:class-id params)))

(defn drop-down-meijerink [meijerink-criteria params]
  (form/drop-down {:onchange "this.form.submit()"}
                  "meijerink"
                  (into [["-- Kies Niveau --" ""]]
                        meijerink-criteria)
                  (:meijerink params)))

(defn render-completion [classes meijerink-criteria domains students params options]
  (let [meijerink-criteria (sort meijerink-criteria)
        domains (sort domains)
        class (first (filter #(= (:class-id params) (:id %)) classes))
        scope (:meijerink params)
        scope (if (str/blank? scope) nil scope)]
    (layout
     (merge {:title (if class
                      (str "Rapport voor \"" (:full-name class) "\"")
                      "Rapport")}
            options)

     [:form {:method "GET"}
      (drop-down-classes classes params)
      (drop-down-meijerink meijerink-criteria params)]

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

(defn render-chapter-list [classes class chapter-list params options]
  (let [selected-chapter-id (:chapter-id params)
        selected-section-id (:section-id params)
        class-id (:class-id params)]
    (layout
     (merge {:title (if class
                      (str "Voortgang voor \"" (:full-name class) "\"")
                      "Voortgang")}
            options)

     [:form {:method "GET"}
      (drop-down-classes classes params)]

     (when class
       [:div#m-teacher_chapter_list
        [:h2 (:name chapter-list)]

        [:nav#teacher_chapter_list_sidenav
         [:ol.chapter-list
          (for [{chapter-title :title chapter-id :id :as chapter} (:chapters chapter-list)]
            [:li {:class (str "chapter " (when (= selected-chapter-id (str chapter-id)) "open"))}
             [:a.chapter-title
              {:href (str "?class-id=" class-id "&chapter-id=" chapter-id)}
              (h chapter-title)]

             (when (= selected-chapter-id (str chapter-id))
               [:ol.section-list
                (for [{section-title :title section-id :id :as section} (:sections chapter)]
                  [:li {:class (str "section" (when (= selected-section-id (str section-id)) "selected"))}
                   [:a.section
                    {:href (str "?class-id=" class-id "&chapter-id=" chapter-id "&section-id=" section-id)}
                    (h section-title)]

                   (when-let [section-counts (get-in chapter-list [:section-counts (str chapter-id) (str section-id)])]
                     [:div.section_status
                      (for [status [:stuck :in-progress :unstarted :finished]]
                        [:span {:class (name status)} (get section-counts status 0)])])])])])]]

        (when-let [section-counts (get-in chapter-list [:section-counts selected-chapter-id selected-section-id])]
          [:div.teacher_chapter_list_main
           (for [status [:stuck :in-progress :unstarted :finished]]
             (for [student (get-in section-counts [:student-list status])]
               [:div.student {:class (name status)} student]))])]))))

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
             class (some (fn [class]
                           (when (= class-id (:id class))
                             (read-model/decorate-class-completion read-model class))) classes)
             students (when class
                        (->> (read-model/students-for-class read-model class)
                             (map (partial read-model/decorate-student-completion read-model))))
             options (assoc flash :redirect-urls redirect-urls)]
         (binding [*current-nav-uri* "/reports/completion"]
           (render-completion classes meijerink-criteria domains students params options))))

  (GET "/reports/chapter-list"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [class-id chapter-id section-id] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             class (some (fn [c]
                           (when (= class-id (:id c))
                             c)) classes)
             chapter-list (when class
                            (read-model/chapter-list read-model class chapter-id section-id))
             options (assoc flash :redirect-urls redirect-urls)]
         (binding [*current-nav-uri* "/reports/chapter-list"]
           (render-chapter-list classes class chapter-list params options))))

  (GET "/reports/export"
       {:keys [read-model teacher]
        {:keys [class-id] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             domains (sort (read-model/domains read-model))
             meijerink-criteria (sort (read-model/meijerink-criteria read-model))
             class (first (filter #(= class-id (:id %)) classes))
             students (when class (read-model/students-for-class read-model class))]
         (render-export classes domains students meijerink-criteria params))))
