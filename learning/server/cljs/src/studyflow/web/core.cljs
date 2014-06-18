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


(defn navigation [app owner]
  (reify
    om/IRender
    (render [_]
      (if-let [course (get app :course-material)]
        (dom/span nil course)
        #_(apply dom/ul nil
                        (map #(dom/li nil %) "abc"))
        (dom/h2 nil "No content ...")))))

(defn content [app owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/h2 nil "Nothing selected ... todo ..."))))

(defn ^:export course-page []
  (om/root
   navigation
   app-state
   {:target (. js/document (getElementById "navigation"))})
  (om/root
   content
   app-state
   {:target (. js/document (getElementById "content"))})
  (.log js/console "Here")
  (service/start-service app-state))
