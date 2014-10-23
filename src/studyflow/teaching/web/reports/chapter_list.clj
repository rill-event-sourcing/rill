(ns studyflow.teaching.web.reports.chapter-list
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [ring.util.codec :refer [url-encode]]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.util :refer :all]
            [rill.uuid :refer [uuid]]))

(defn render-chapter-list [class classes chapter-list params options]
  (let [selected-chapter-id (uuid (:chapter-id params))
        selected-section-id (uuid (:section-id params))]
    (layout
     options
     (drop-list-classes classes nil "chapter-list" (:class-name class))
     class

     [:h1#page-title "Hoofdstukken"]
     (when class
       [:div#m-teacher_chapter_list
        [:nav#teacher_chapter_list_sidenav
         [:ol.chapter-list
          (for [{chapter-title :title chapter-id :id :as chapter} (:chapters chapter-list)]
            [:li {:class (str "chapter" (when (= selected-chapter-id (str chapter-id)) " open"))}
             [:a.chapter-title
              {:href (chapter-list-url class chapter-id nil)}
              (h chapter-title)]

             [:div.chapter_status
              (let [total-students-stuck-in-chapter (get-in chapter-list [:chapters-completion chapter-id :stuck])]
                (when-not (= 0 total-students-stuck-in-chapter)
                  [:div
                   [:span {:class "stuck_sign"} (str total-students-stuck-in-chapter)]
                   [:span {:class "warning_sign"} "&#9888;"]]))
              (let [students-who-completed-this-chapter (get-in chapter-list [:chapters-with-finishing-data chapter-id] 0)
                    total-students (get-in chapter-list [:total-number-of-students])]
                [:span (str (Math/round (float (/ (* 100 students-who-completed-this-chapter) total-students)))
                            "%")])]

             (when (= selected-chapter-id chapter-id)
               [:ol.section-list
                (for [{section-title :title section-id :id :as section} (:sections chapter)]
                  [:li {:class (str "section" (when (= selected-section-id section-id) " selected"))}
                   [:a.section_link
                    {:href (chapter-list-url class chapter-id section-id)}
                    (h section-title)]

                   (when-let [section-counts (get-in chapter-list [:sections-total-status chapter-id section-id])]
                     [:div.section_status
                      (let [students-stuck-in-section (get section-counts :stuck)]
                        (when students-stuck-in-section
                          [:div
                           [:span {:class "stuck_sign"} students-stuck-in-section]
                           [:span {:class "warning_sign"} "&#9888;"]]))
                      (let [finished-students (get section-counts :finished 0)
                            total-students (reduce + 0 (map
                                                        (fn [status] (get section-counts status 0))
                                                        [:stuck :in-progress :unstarted :finished]))]
                        [:div {:class "progress"}
                         [:div {:class "progress_bar"
                                :style (str "width:"
                                            (Math/round (float (/ (* 100 finished-students) total-students)))
                                            "%;")}
                          [:span (str finished-students "/" total-students)]]])])])])])]]

        (when-let [section-counts (get-in chapter-list [:sections-total-status selected-chapter-id selected-section-id])]
          [:div.teacher_chapter_list_main
           (for [status [:stuck :in-progress :unstarted :finished]]
             (for [student (sort-by :full-name (get-in section-counts [:student-list status]))]
               [:div.student {:class (name status)} (:full-name student)
                [:span.time-spent (time-spent-html (:time-spent student))]]))])]))))

(defn chapter-list [read-model teacher redirect-urls params]
  (let [classes (read-model/classes read-model teacher)
        class (some (fn [c] (when (= (:class-id params) (:id c)) c)) classes)
        chapter-list (when class (read-model/chapter-list read-model
                                                          class
                                                          (uuid (:chapter-id params))
                                                          (uuid (:section-id params))))
        options {:redirect-urls redirect-urls
                 :title (if class
                          (str "Hoofdstukken voor \"" (:class-name class) "\"")
                          "Hoofdstukken")}]
    (binding [*current-report-name* "chapter-list"]
      (render-chapter-list class classes chapter-list params options))))

(defroutes chapter-list-routes

  (GET "/reports/chapter-list"
       {:keys [read-model teacher redirect-urls]}
       (chapter-list read-model teacher redirect-urls nil))

  (GET "/reports/:class-id/chapter-list"
       {:keys [read-model teacher redirect-urls]
        params :params}
       (chapter-list read-model teacher redirect-urls params))

  (GET "/reports/:class-id/chapter-list/:chapter-id"
       {:keys [read-model teacher redirect-urls]
        params :params}
       (chapter-list read-model teacher redirect-urls params))

  (GET "/reports/:class-id/chapter-list/:chapter-id/:section-id"
       {:keys [read-model teacher redirect-urls]
        params :params}
       (chapter-list read-model teacher redirect-urls params)))
