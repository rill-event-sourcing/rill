(ns studyflow.teaching.web.reports.chapter-list
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.util :refer :all]
            [rill.uuid :refer [uuid]]))

(defn render-chapter-list [class classes chapter-list params options]
  (let [selected-chapter-id (uuid (:chapter-id params))
        selected-section-id (uuid (:section-id params))
        class-id (:class-id params)
        report-name "chapter-list"]
    (layout
     (merge {:title (if class
                      (str "Hoofdstukken voor \"" (:full-name class) "\"")
                      "Hoofdstukken")}
            options)

     (drop-list-classes classes nil report-name)

     (when class
       [:div#m-teacher_chapter_list
        [:nav#teacher_chapter_list_sidenav
         [:ol.chapter-list
          (for [{chapter-title :title chapter-id :id :as chapter} (:chapters chapter-list)]
            [:li {:class (str "chapter" (when (= selected-chapter-id (str chapter-id)) " open"))}
             [:a.chapter-title
              {:href (str "/reports/" (:id class) "/chapter-list/" chapter-id)}
              (h chapter-title)]

             (when (= selected-chapter-id chapter-id)
               [:ol.section-list
                (for [{section-title :title section-id :id :as section} (:sections chapter)]
                  [:li {:class (str "section" (when (= selected-section-id section-id) " selected"))}
                   [:a.section_link
                    {:href (str "/reports/" (:id class) "/chapter-list/" chapter-id "/" section-id)}
                    (h section-title)]

                   (when-let [section-counts (get-in chapter-list [:section-counts chapter-id section-id])]
                     [:div.section_status
                      (for [status [:stuck :in-progress :unstarted :finished]]
                        [:span {:class (name status)} (get section-counts status 0)])])])])])]]

        (when-let [section-counts (get-in chapter-list [:section-counts selected-chapter-id selected-section-id])]
          [:div.teacher_chapter_list_main
           (for [status [:stuck :in-progress :unstarted :finished]]
             (for [student (sort-by :full-name (get-in section-counts [:student-list status]))]
               [:div.student {:class (name status)} (:full-name student)
                [:span.time-spent (time-spent-html (:time-spent student))]]))])]))))

(defn chapter-list [read-model flash teacher redirect-urls params class-id chapter-id section-id]
  (let [chapter-id (uuid chapter-id)
        section-id (uuid section-id)
        classes (read-model/classes read-model teacher)
        class (some (fn [c] (when (= class-id (:id c)) c)) classes)
        chapter-list (when class (read-model/chapter-list read-model class chapter-id section-id))
        options (assoc flash :redirect-urls redirect-urls)]
    (binding [*current-nav-uri* "/reports/chapter-list"]
      (render-chapter-list class classes chapter-list params options))))

(defroutes chapter-list-routes

  (GET "/reports/chapter-list"
       {:keys [read-model flash teacher redirect-urls]}
       (chapter-list read-model flash teacher redirect-urls nil nil nil nil))

  (GET "/reports/:class-id/chapter-list"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [class-id] :as params} :params}
       (chapter-list read-model flash teacher redirect-urls params class-id nil nil))

  (GET "/reports/:class-id/chapter-list/:chapter-id"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [class-id chapter-id] :as params} :params}
       (chapter-list read-model flash teacher redirect-urls params class-id chapter-id nil))

  (GET "/reports/:class-id/chapter-list/:chapter-id/:section-id"
       {:keys [read-model flash teacher redirect-urls]
        {:keys [class-id chapter-id section-id] :as params} :params}
       (chapter-list read-model flash teacher redirect-urls params class-id chapter-id section-id)))


