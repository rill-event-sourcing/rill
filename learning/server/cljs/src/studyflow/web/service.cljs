(ns studyflow.web.service
  (:require [ajax.core :refer [GET POST] :as ajax-core]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.json-edn :as json-edn]))

;; a bit silly to use an Om component for something that is not UI,
;; but don't know how to participate in state managament otherwise
(defn start-service [app-state]
  (om/root
   (fn [cursor owner]
     (reify
       om/IRender
       (render [_]
         (dom/div nil nil))
       om/IWillMount
       (will-mount [_]
         (.setTimeout js/window
          #(GET (str "/api/course-material/"
                    (get-in @cursor [:course-id]))
               {:params {}
                :handler (fn [res]
                           (println "Service heard: " res)
                           (let [course-data (json-edn/json->edn res)]
                             (om/update! cursor :course-material course-data)))
                :error-handler (fn [res]
                                 (println "Error handler" res)
                                 (println res))})
          1000))))
   app-state
   {:target (. js/document (getElementById "services"))
    :tx-listen (fn [tx-report cursor]
                 (let [{:keys [path new-state]} tx-report]
                   (cond
                    (= path [:selected-section])
                    (if-let [[chapter-id section-id] (get-in new-state [:selected-section])]
                      (if-let [section-data (get-in new-state [:sections-data section-id])]
                       nil ;; data already loaded
                       (GET (str "/api/course-material/"
                                 (get-in new-state [:course-id])
                                 "/chapter/" chapter-id
                                 "/section/" section-id)
                            {:params {}
                             :handler (fn [res]
                                        (println "Service heard: " res)
                                        (let [section-data (json-edn/json->edn res)]
                                          (println "section: " section-data)
                                          (om/transact! cursor
                                                        #(assoc-in %
                                                                   [:sections-data (:id section-data)]
                                                                   section-data))))
                              :error-handler (fn [res]
                                               (println "Error handler" res)
                                               (println res))}))
                     nil ;; maybe preload here?
                     ))))}))

