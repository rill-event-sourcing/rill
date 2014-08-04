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

(defn basic-error-handler [res]
  (println "Error handler" res)
  (println res))

(defn command-error-handler [cursor]
  (fn [res]
    (om/update! cursor
                [:aggregates :failed]
                true)))

(defn command-section-test-aggregate-handler [cursor section-id]
  (fn [res]
    (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
      (om/transact! cursor
                    [:aggregates section-id]
                    (fn [agg]
                      (aggregates/apply-events agg aggregate-version events))))))

(defn handle-replay-events [cursor section-id events aggregate-version]
  (om/transact! cursor
                [:aggregates section-id]
                (fn [agg]
                  (if (seq events)
                    (aggregates/apply-events agg aggregate-version events)
                    nil))))

(defn try-command [cursor command]
  (prn :try-command command)
  (let [[command-type & args] command]
    (condp = command-type
      "section-test-commands/init-when-nil"
      (let [[section-id student-id] args
            course-id (get-in @cursor [:static :course-id])]
        (prn "DO section-test-init?" (get-in @cursor [:aggregates section-id]) (contains? (get @cursor :aggregates) section-id))
        (when (nil? (get-in @cursor [:aggregates section-id]))
          (om/update! cursor [:aggregates section-id] false)
          (PUT (str "/api/section-test-init/" course-id "/" section-id "/" student-id)
               {:format :json
                :handler (command-section-test-aggregate-handler cursor section-id)
                :error-handler (command-error-handler cursor)
                })))

      "section-test-commands/check-answer"
      (let [[section-id student-id section-test-aggregate-version course-id question-id inputs] args]
        (PUT (str "/api/section-test-check-answer/" section-id "/" student-id "/" course-id "/" question-id)
             {:params {:expected-version section-test-aggregate-version
                       :inputs inputs}
              :format :json
              :handler (command-section-test-aggregate-handler cursor section-id)
              :error-handler (command-error-handler cursor)}))
      "section-test-commands/next-question"
      (let [[section-id student-id section-test-aggregate-version course-id] args]
        (PUT (str "/api/section-test-next-question/" section-id "/" student-id "/" course-id)
             {:params {:expected-version section-test-aggregate-version}
              :format :json
              :handler (command-section-test-aggregate-handler cursor section-id)
              :error-handler (command-error-handler cursor)}))
      nil)))

(defn load-data [cursor command]
  (prn :data-command command)
  (let [[command-type & args] command]
    (condp = command-type
      "data/dashboard"
      (when-not (get-in @cursor [:view :course-material])
        (GET (str "/api/course-material/"
                  (get-in @cursor [:static :course-id]))
             {:params {}
              :handler (fn [res]
                         (let [course-data (json-edn/json->edn res)]
                           (om/update! cursor
                                       [:view :course-material] course-data)))
              :error-handler basic-error-handler}))
      "data/navigation"
      (let [[chapter-id student-id] args]
        (prn "chapter-id: " chapter-id)
        (prn "chapter from: " (get-in @cursor [:view :course-material :chapters]))
        (let [chapter (some (fn [chapter]
                              (when (= (:id chapter) chapter-id)
                                chapter))
                            (get-in @cursor [:view :course-material :chapters]))]
          (doseq [section-id (map :id (:sections chapter))]
            (prn "Load aggregate: " section-id)
            (when-not (contains? (get @cursor :aggregates) section-id)
              (om/update! cursor [:aggregates section-id] false)
              (GET (str "/api/section-test-replay/" section-id "/" student-id)
                   {:format :json
                    :handler (fn [res]
                               (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
                                 (handle-replay-events cursor section-id events aggregate-version)))
                    :error-handler (fn [res]
                                     ;; currently the api
                                     ;; gives a 401 when
                                     ;; there are no events
                                     ;; for an aggregate
                                     (handle-replay-events cursor section-id [] -1)
                                     )})))))
      "data/section-explanation"
      (let [_ (prn "section explanation get data" args)
            [chapter-id section-id] args
            course-id (get-in @cursor [:static :course-id])]
        (if-let [section-data (get-in @cursor [:view :section section-id :data])]
          nil ;; data already loaded
          (GET (str "/api/course-material/"
                    course-id
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
                :error-handler basic-error-handler})))
      "data/section-test"
      nil ;; command if not yet loaded
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
        ;; todo

        ;; commands from UI
        (let [command-channel (om/get-shared owner :command-channel)]
            (go (loop []
               (when-let [command (<! command-channel)]
                 (try-command cursor command)
                 (recur)))))

        ;; data requests from UI
        (let [data-channel (om/get-shared owner :data-channel)]
          (go (loop []
                (when-let [data-request (<! data-channel)]
                  (load-data cursor data-request)
                  (recur))))))
      om/IRender
      (render [_]
        (om/build widgets cursor)))))

(defn find-event [name events]
  (first (filter #(gstring/endsWith (:type %) name) events)))

(defn listen [tx-report cursor]
  (let [{:keys [path new-state]} tx-report]
    (cond
     (let [[view section _ test _ & more] path]
       (and (not more)
            (= view :view)
            (= section :section)
            (= test :test)))
     (let [[_ _ section-id _ question-id] path
           chapter-id (get-in new-state [:view :selected-path :chapter-id])]
       (GET (str "/api/course-material/"
                 (get-in new-state [:static :course-id])
                 "/chapter/" chapter-id
                 "/section/" section-id
                 "/question/" question-id)
            {:params {}
             :handler (fn [res]
                        (let [question-data (json-edn/json->edn res)]
                          (om/transact! cursor
                                        #(assoc-in %
                                                   [:view :section section-id :test (:id question-data)]
                                                   question-data))))
             :error-handler basic-error-handler})))

    :else nil))
