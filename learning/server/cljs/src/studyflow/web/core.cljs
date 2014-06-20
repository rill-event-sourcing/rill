(ns studyflow.web.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.service :as service]
            [studyflow.web.qservice :as qservice]
            [clojure.string :as string]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as qdom]))


(enable-console-print!)

(defn course-id-for-page []
  (let [loc (.. js/window -location)]
    (last (string/split loc "/"))))

(def app-state (atom {:course-id  (course-id-for-page)}))


(defn navigation [cursor owner]
  (reify
    om/IRender
    (render [_]
      (if-let [course (get cursor :course-material)]
        (dom/div nil
                 (dom/h1 #js {:data-id (:id course)} (:name course))
                 (apply dom/ul nil
                        (for [{:keys [title sections]
                               chapter-id :id
                               :as chapter} (:chapters course)]
                          (dom/li #js {:data-id chapter-id}
                                  title
                                  (apply dom/ul nil
                                         (for [{:keys [title]
                                                section-id :id
                                                :as section} sections]
                                           (dom/a #js {:onClick
                                                       (fn [_]
                                                         (om/update! cursor
                                                                     :selected-section
                                                                     [chapter-id section-id]))}
                                                  (dom/li #js {:data-id section-id}
                                                          title))))))))
        (dom/h2 nil "No content ... spinner goes here")))))

(defn content [cursor owner]
  (reify
    om/IRender
    (render [_]
      (if-let [[_ section-id] (get-in cursor [:selected-section])]
        (if-let [section-data (get-in cursor [:sections-data section-id])]
          (dom/div nil (str "Section data:" (pr-str section-data)))
          (dom/h2 nil (str "Selected data not yet loaded" section-id)))
        (dom/h2 nil "No section selected")))))

(defn ^:export course-page []
  (om/root
   navigation
   app-state
   {:target (. js/document (getElementById "navigation"))
    ;; hack around issue OM-170
    :tx-listen (fn [tx-report cursor])})
  (om/root
   content
   app-state
   {:target (. js/document (getElementById "content"))})
  (service/start-service app-state))


(def qstate (atom {:course-id  (course-id-for-page)}))

(q/defcomponent Navigation [state qstate]
  (if-let [course (:course-material state)]
    (qdom/div {} "Course-material available: "
              (qdom/h1 {:data-id (:id course)} (:name course))
              (apply qdom/ul nil
                     (for [{:keys [title sections]
                            chapter-id :id
                            :as chapter} (:chapters course)]
                       (qdom/li #js {:data-id chapter-id}
                                title
                                (apply qdom/ul nil
                                       (for [{:keys [title]
                                              section-id :id
                                              :as section} sections]
                                         (qdom/a #js {:onClick
                                                      (fn [_]
                                                        (println "onclick" (pr-str state))
                                                        (swap! qstate
                                                               assoc :selected-section
                                                               [chapter-id section-id]
                                                               :event [:navigation-selection]))}
                                                 (dom/li #js {:data-id section-id}
                                                         title))))))))
    (qdom/h1 {} "No navigation available")))

(q/defcomponent Content [state]
  (if-let [[_ section-id] (get-in state [:selected-section])]
    (if-let [section-data (get-in state [:sections-data section-id])]
      (dom/div nil (str "Section data:" (pr-str section-data)))
      (dom/h2 nil (str "Selected data not yet loaded" section-id)))
    (dom/h2 nil "No section selected")))

(q/defcomponent App
  [state]
  (qdom/div {}
            (Navigation state qstate)
            (Content state)))

(defn ^:export course-page-quiescent []
  (.log js/console "Hello quiescent")
  (add-watch qstate :quiescent
             (fn [_ _ old state]
               ;; Queue with animationFrame, through goog closure
               (q/render (App state qstate)
                         (.getElementById js/document "content"))))
  (qservice/start-service qstate)
  (swap! qstate identity))
