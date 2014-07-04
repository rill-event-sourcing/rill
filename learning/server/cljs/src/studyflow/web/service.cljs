(ns studyflow.web.service
  (:require [ajax.core :refer [GET POST PUT] :as ajax-core]
            [cljs-uuid.core :as uuid]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.json-edn :as json-edn]
            [goog.string :as gstring]
            [studyflow.web.aggregates :as aggregates]
            [cljs.core.async :refer [<!] :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn parse-history-hash [history-hash]
  "#chapter-12334-2342-2342134-123412section-1243234-21421-1243-124"
  (let [chapter-id (-> history-hash
                       (.replace #"section-.*$" "")
                       (.replace "#chapter-" ""))
        section-id (.replace history-hash #".*section-" "")]
    [chapter-id section-id]))

(defn try-command [cursor command]
  (prn :try-command command)
  (let [[command-type & args] command]
    (condp = command-type
      "section-test-commands/init"
      (let [[section-id] args
            course-id (get-in @cursor [:static :course-id])
            section-test-id (. (uuid/make-random) -uuid)]
        (om/update! cursor
                    [:view :section section-id :test :id]
                    section-test-id)
        (PUT (str "/api/section-test-init/" course-id "/" section-id "/" section-test-id)
             {:handler (fn [res]
                         (let [events (:events (json-edn/json->edn res))
                               section-test-id (:section-test-id (find-event "/Created" events))
                               question-id (:question-id (find-event "/QuestionAssigned" events))]
                           (om/transact! cursor
                                         [:aggregates section-test-id]
                                         (fn [agg]
                                           (prn "playing events on: " agg " with aggr-id " section-test-id " with events: " events)
                                           (aggregates/apply-events agg events)))))}))
     "section-test-commands/check-answer"
     (let [[section-test-id section-id course-id question-id inputs] args]
       (PUT (str "/api/section-test-check-answer/" section-test-id "/"  section-id "/" course-id "/" question-id)
            {:params inputs
             :format :json
             :handler (fn [res]
                        (let [events (:events (json-edn/json->edn res))]
                          (om/transact! cursor
                                        [:aggregates section-test-id]
                                        (fn [agg]
                                          (prn "playing events on: " agg " with aggr-id " section-test-id " with events: " events)
                                          (let [res (aggregates/apply-events agg events)]
                                            (prn "RES: " res)
                                            res)))))}))
     "section-test-commands/next-question"
     (let [[section-test-id] args]
       ;; mock server response
       (let [events [{:id (. (uuid/make-random) -uuid)
                      :type "studyflow.learning.section-test.events/QuestionAssigned"
                      :question-id "b117bf7b-8025-43ea-b6d3-aa636d6b6042"
                      :section-test-id section-test-id}]]
         (om/transact! cursor
                       [:aggregates section-test-id]
                       (fn [agg]
                         (prn "playing events on: " agg " with aggr-id " section-test-id " with events: " events)
                         (let [res (aggregates/apply-events agg events)]
                           (prn "RES: " res)
                           res)))))
     nil)))

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
        (let [command-channel (om/get-shared owner :command-channel)]
            (go (loop []
               (when-let [command (<! command-channel)]
                 (try-command cursor command)
                 (recur)))))


        ;; initialize the menu
        (GET (str "/api/course-material/"
                  (get-in cursor [:static :course-id]))
             {:params {}
              :handler (fn [res]
                         (println "Service heard: " res)
                         (let [course-data (json-edn/json->edn res)]
                           (om/update! cursor
                                       [:view :course-material] course-data)))
              :error-handler (fn [res]
                               (println "Error handler" res)
                               (println res))})
        (let [history-hash (.. js/document -location -hash)
              [chapter-id section-id] (parse-history-hash history-hash)]
          (prn "chapter-id from url" chapter-id)
          (prn "section-id from url" section-id)
          (when (and chapter-id
                     section-id)
            (om/update! cursor
                        [:view :selected-section]
                        [chapter-id section-id]))))
      om/IRender
      (render [_]
        (om/build widgets cursor)))))

(defn find-event [name events]
  (first (filter #(gstring/endsWith (:type %) name) events)))



(defn listen [tx-report cursor]
  (let [{:keys [path new-state]} tx-report]
    (cond
     (= path [:view :selected-section])
     (if-let [[chapter-id section-id] (get-in new-state [:view :selected-section])]
       (if-let [section-data (get-in new-state [:view :section section-id :data])]
         nil ;; data already loaded
         (do
           (let [section-test-id (get-in new-state [:view :section section-id :test :id])]
             ;; where to get the section-test-id from? this won't be
             ;; on the client after reload
             (prn "Load aggregate: " section-test-id)
             (when-not (contains? (get-in new-state [:aggregates section-test-id]))
               (GET (str "/api/section-test-replay/" section-test-id)
                    {:format :json
                     :handler (fn [res]
                                (let [events (:events (json-edn/json->edn res))]
                                  (om/transact! cursor
                                                [:aggregates section-test-id]
                                                (fn [agg]
                                                  (prn "playing events on: " agg " with aggr-id " section-test-id " with events: " events)
                                                  (let [res (aggregates/apply-events agg events)]
                                                    (prn "RES: " res)
                                                    res)))))})))
           (GET (str "/api/course-material/"
                     (get-in new-state [:static :course-id])
                     "/chapter/" chapter-id
                     "/section/" section-id)
               {:params {}
                :handler (fn [res]
                           (println "Service heard: " res)
                           (let [section-data (json-edn/json->edn res)]
                             (println "section: " section-data)
                             (om/transact! cursor
                                           #(assoc-in %
                                                      [:view :section (:id section-data) :data]
                                                      section-data))))
                :error-handler (fn [res]
                                 (println "Error handler" res)
                                 (println res))})))
       nil)

     :else nil)))
