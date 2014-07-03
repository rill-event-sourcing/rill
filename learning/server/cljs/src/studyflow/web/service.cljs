(ns studyflow.web.service
  (:require [ajax.core :refer [GET POST PUT] :as ajax-core]
            [cljs-uuid.core :as uuid]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.json-edn :as json-edn]
            [goog.string :as gstring]))

;; a bit silly to use an Om component for something that is not UI,
;; but don't know how to participate in state managament otherwise
;; root component for services seems to be the default om pattern:
;; https://groups.google.com/forum/#!topic/clojurescript/DHJvcGey8Sc

(defn wrap-service [widgets]
  (fn [cursor owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (println "service will mount")
        ;; listen to server push connection here
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
                     1000))
      om/IRender
      (render [_]
        (om/build widgets cursor)))))

(defn find-event [name events]
  (first (filter #(gstring/endsWith (:type %) name) events)))

(defn try-command [cursor command]
  (prn :try-command command)
  (let [handler ({"section-test-commands/check-answer"
                  (fn [[section-test-id section-id course-id question-id inputs]]
                    (PUT (str "/api/section-test-check-answer/" section-test-id "/"  section-id "/" course-id "/" question-id)
                         {:params inputs
                          :format :json
                          :handler (fn [res]
                                     (let [events (:events (json-edn/json->edn res))]
                                       (cond
                                        (find-event "/QuestionAnsweredCorrectly" events)
                                        (js/alert "GOED!")

                                        (find-event "/QuestionAnsweredIncorrectly" events)
                                        (js/alert "jammer.."))))}))}
                 (first command))]
    (handler (next command))))

(defn listen [tx-report cursor]
  (let [{:keys [path new-state]} tx-report]
    (condp = path
      [:selected-section]
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
        nil)

      [:section-id-for-section-test]
      (when-let [section-id (:section-id-for-section-test new-state)]
        (PUT (str "/api/section-test-init/" (:course-id new-state) "/" section-id "/"  (. (uuid/make-random) -uuid))
             {:handler (fn [res]
                         (let [events (:events (json-edn/json->edn res))
                               section-test-id (:section-test-id (find-event "/Created" events))
                               question-id (:question-id (find-event "/QuestionAssigned" events))]
                           (om/transact! cursor
                                         #(assoc %
                                            :current-section-test-id section-test-id
                                            :current-section-test-question-id question-id))))}))

      [:command-queue]
      (when (first (:command-queue new-state))
        (om/transact! cursor [:command-queue]
                      #(do
                         (try-command cursor (first %))
                         (next %))))

      nil)))
