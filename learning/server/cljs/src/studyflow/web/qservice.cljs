(ns studyflow.web.qservice
  (:require [ajax.core :refer [GET POST] :as ajax-core]
            [studyflow.web.json-edn :as json-edn]))

(defn start-service [app-state]
  (add-watch app-state :service
             (fn [k a old state]
               (let [{:keys [path new-state]} tx-report]
                   (cond
                    (not= (:selected-section old) (:selected-section state))
                    (if-let [[chapter-id section-id] (get-in state [:selected-section])]
                      (if-let [section-data (get-in state [:sections-data section-id])]
                        nil ;; data already loaded
                        (GET (str "/api/course-material/"
                                  (get-in state [:course-id])
                                  "/chapter/" chapter-id
                                  "/section/" section-id)
                             {:params {}
                              :handler (fn [res]
                                         (println "Service heard: " res)
                                         (let [section-data (json-edn/json->edn res)]
                                           (println "section: " section-data)
                                           (swap! app-state
                                                  (fn [old]
                                                    (-> old
                                                        (assoc-in [:sections-data (:id section-data)]
                                                                  section-data)
                                                        (assoc :event [:service-load-section (:id section-data)]))))))
                              :error-handler (fn [res]
                                               (println "Error handler" res)
                                               (println res))}))
                      nil ;; maybe preload here?
                      )))))
  (.setTimeout js/window
               #(GET (str "/api/course-material/"
                          (get-in @app-state [:course-id]))
                     {:params {}
                      :handler (fn [res]
                                 (println "Service heard: " res)
                                 (let [course-data (json-edn/json->edn res)]
                                   (swap! app-state assoc :course-material course-data
                                          :event [:service-on-load])))
                      :error-handler (fn [res]
                                       (println "Error handler" res)
                                       (println res))})
               1000))

