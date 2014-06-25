(ns studyflow.web.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.service :as service]
            [clojure.string :as string]))


(enable-console-print!)

(defn course-id-for-page []
  (let [loc (.. js/window -location)]
    (last (string/split loc "/"))))

(def app-state (atom {:course-id  (course-id-for-page)}))


(defn navigation [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "Navigation will mount"))
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
        (dom/h2 nil "No content ... spinner goes here")))
    om/IWillUnmount
    (will-unmount [_]
      (println "Navigation will unmount"))))

(defn content [cursor owner]
  (reify
    om/IRender
    (render [_]
      (if-let [[_ section-id] (get-in cursor [:selected-section])]
        (if-let [section-data (get-in cursor [:sections-data section-id])]
          (dom/div nil (str "Section data:" (pr-str section-data)))
          (dom/h2 nil (str "Selected data not yet loaded" section-id)))
        (dom/h2 nil "No section selected")))))

(defn widgets [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "widget will mount"))
    om/IRender
    (render [_]
      (dom/div nil
               (om/build navigation cursor)
               (om/build content cursor)))
    om/IWillUnmount
    (will-unmount [_]
      (println "widget will unmount"))))

(defn ^:export course-page []
  (om/root
   (service/wrap-service widgets)
   app-state
   {:target (. js/document (getElementById "app"))
    :tx-listen service/listen}))
