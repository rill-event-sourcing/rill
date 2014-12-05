(ns studyflow.web.dashboard
  (:require [om.dom :as dom]
            [om.core :as om]
            [studyflow.web.history :refer [path-url]]
            [studyflow.web.recommended-action :refer [recommended-action]]
            [studyflow.web.chapter-quiz :as chapter-quiz]
            [studyflow.web.helpers :refer [input-builders tool-box modal raw-html tag-tree-to-om focus-input-box section-explanation-url] :as helpers]
            [cljs.core.async :as async]))

(defn sections-navigation [cursor chapter]
  (apply dom/ol #js {:id "section_list"}
         (let [chapter-id (:id chapter)
               rcm-action (recommended-action cursor)
               recommended-id (:id rcm-action)]
           (concat
            (for [{:keys [title status]
                   section-id :id
                   :as section} (:sections chapter)]
              (let [section-status (get {"finished" "finished"
                                         "stuck" "stumbling_block"
                                         "in-progress" "in_progress"} status "")
                    section-link (section-explanation-url cursor chapter section)]
                (dom/li #js {:data-id section-id
                             :className (str "section_list_item " section-status
                                             (when (= recommended-id section-id) " recommended")) }
                        (dom/a #js {:href section-link
                                    :className (str "section_link "
                                                    section-status)}
                               title)
                        (dom/a #js {:className "btn blue chapter_nav_btn"
                                    :href section-link} "Start"))))
            [(chapter-quiz/chapter-quiz-navigation-button cursor (:chapter-quiz chapter) chapter-id)]))))

(defn chapter-navigation [cursor selected-chapter-id course chapter]
  (let [selected? (= selected-chapter-id (:id chapter))]
    (dom/li #js {:className (str "chapter_list_item"
                                 (when (= (:status chapter) "finished") " finished")
                                 (when selected? " open"))}
            (dom/a #js {:data-id (:id course)
                        :className "chapter_title"
                        :href (-> (get-in cursor [:view :selected-path])
                                  (assoc :chapter-id (:id chapter)
                                         :section-id nil
                                         :main :dashboard)
                                  path-url)}
                   (:title chapter))
            (when selected?
              (sections-navigation cursor chapter)))))



(defn sidenav
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/aside #js {:id "m-sidenav"}
                 (dom/div #js {:id "student_info"} (get-in cursor [:static :student :full-name]))
                 (dom/div #js {:id "recommended_action"}
                          (let [{:keys [title link]} (recommended-action cursor)]
                            (dom/div nil
                                     (dom/span nil "Ga verder met:")
                                     (dom/p #js {:id "recommended_title"} title)
                                     (dom/a #js {:id "recommended_button"
                                                 :className "btn big yellow" :href link} "Start"))))
                 (dom/div #js {:id "support_info"})))))

(defn top-header
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/header #js {:id "m-top_header"}
                  (dom/h1 #js {:id "logo"} (:name (get-in cursor [:view :course-material])))
                  (dom/a #js {:id "help" :href "#"})
                  (dom/a #js {:id "settings" :href "#"})
                  (when-let [url (get-in cursor [:static :teaching-url])]
                    (dom/a #js {:id "teaching" :href url} "Docent omgeving"))
                  (dom/form #js {:method "POST"
                                 :action (get-in cursor [:static :logout-target])
                                 :id "logout-form"}
                            (dom/input #js {:type "hidden"
                                            :name "_method"
                                            :value "DELETE"})
                            (dom/button #js {:type "submit"}
                                        "Uitloggen"))))))
(defn navigation [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :data-channel)
                    ["data/navigation" chapter-id student-id])))
    om/IRender
    (render [_]
      (let [course (get-in cursor [:view :course-material])
            chapter-id (or (get-in cursor [:view :selected-path :chapter-id]) (:id (first (:chapters course))))]

        (if course
          (dom/div nil
                   (dom/nav #js {:id "m-dashboard_chapter_nav"}
                            (dom/h1 #js {:className "chapter_nav_title"} "Mijn Leerroute")
                            (apply dom/ol #js {:className "chapter_list"}
                                   (let [{:keys [name status]
                                          entry-quiz-id :id
                                          :as entry-quiz} (get-in cursor [:view :course-material :entry-quiz])
                                          status (keyword status)]
                                     (when (not (#{:passed :failed} status))
                                       (dom/li #js {:className "chapter_list_item"}
                                               (dom/a #js {:className "chapter_title"
                                                           :href (path-url {:main :entry-quiz})}
                                                      "Instaptoets"))))
                                   (map (partial chapter-navigation cursor chapter-id course)
                                        (:chapters course))))
                   (dom/div #js {:id "m-path"}))
          (dom/h2 nil "Hoofdstukken laden..."))))))

(defn dashboard [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "dashboard_page"}
               (om/build top-header cursor)
               (om/build sidenav cursor)
               (dom/section #js {:id "main"}
                            (om/build navigation cursor))))))

