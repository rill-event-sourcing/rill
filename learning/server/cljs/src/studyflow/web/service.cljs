(ns studyflow.web.service
  (:require [ajax.core :refer [GET POST] :as ajax-core]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
         (GET (str "/api/course-material/"
                   (get-in cursor [:course-id]))
              {:params {}
               :handler (fn [res]
                          (println "Service heard: " res))
               :error-handler (fn [res]
                                (println "Error handler" res))}))))
   app-state
   {:target (. js/document (getElementById "services"))
    :tx-listen (fn [tx-report cursor]
                 (println "services: a b in :tx-listen"
                          (pr-str [tx-report @cursor])))}))

