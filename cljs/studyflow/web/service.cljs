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

(defn add-forward-section-links [course-data]
  (let [section-links (for [chapter (:chapters course-data)
                            section (:sections chapter)]
                        {:chapter-id (:id chapter)
                         :section-id (:id section)})
        forward-links (-> []
                          (into (rest section-links))
                          ;; last section links back to dashboard
                          (conj {:chapter-id nil
                                 :section-id nil}))
        section-links (zipmap section-links forward-links)]
    (assoc course-data :forward-section-links section-links)))

(defn add-chapter-index-mapping [course-data]
  (let [chapters-by-id (into {} (map (juxt :id identity) (:chapters course-data)))]
    (assoc course-data :chapters-by-id chapters-by-id)))

(defn ignore-handler [res])

(defn basic-error-handler [res]
  (println "Error handler" res)
  (println res))

(defn command-error-handler [cursor]
  (fn [res]
    (prn [:ERROR! res])
    (om/update! cursor
                [:aggregates :failed]
                true)))

(defn command-aggregate-handler [cursor notification-channel aggregate-id]
  (fn [res]
    (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
      (om/transact! cursor
                    [:aggregates aggregate-id]
                    (fn [agg]
                      (aggregates/apply-events agg aggregate-version events)))
      (when-let [notification-events
                 (seq (filter
                       (comp #{"studyflow.learning.section-test.events/Finished"
                               "studyflow.learning.section-test.events/StreakCompleted"
                               "studyflow.learning.section-test.events/Stuck"
                               "studyflow.learning.section-test.events/QuestionAnsweredIncorrectly"
                               "studyflow.learning.chapter-quiz.events/QuestionAssigned"
                               "studyflow.learning.chapter-quiz.events/Stopped"} :type)
                       events))]
        (doseq [event notification-events]
          (async/put! notification-channel event))))))

(defn handle-replay-events [cursor aggregate-id events aggregate-version]
  (om/transact! cursor
                [:aggregates aggregate-id]
                (fn [_]
                  (if (seq events)
                    (aggregates/apply-events nil aggregate-version events)
                    nil))))

(defn try-command [cursor notification-channel command-channel command]
  (prn :try-command command)
  (let [[command-type & args] command]
    (condp = command-type
      "section-test-commands/init-when-nil"
      (let [[section-id student-id] args
            course-id (get-in @cursor [:static :course-id])]
        (prn "DO section-test-init?" (get-in @cursor [:aggregates section-id]) (contains? (get @cursor :aggregates) section-id))
        (GET (str "/api/section-test-replay/" section-id "/" student-id)
             {:format :json
              :handler (fn [res]
                         ;; when there's any event the section-test
                         ;; was already started
                         (let [{:keys [events]} (json-edn/json->edn res)]
                           (if events
                             ;; may have started the session
                             ;; previously, but the aggregate has not
                             ;; yet been loaded
                             (when-not (get-in @cursor [:aggregates section-id])
                               (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
                                 (handle-replay-events cursor section-id events aggregate-version)))
                             ;; need to start the section
                             (PUT (str "/api/section-test-init/" course-id "/" section-id "/" student-id)
                                  {:format :json
                                   :handler (command-aggregate-handler cursor notification-channel section-id)
                                   :error-handler (command-error-handler cursor)
                                   }))))
              :error-handler basic-error-handler}))

      "section-test-commands/reveal-worked-out-answer"
      (let [[section-id student-id section-test-aggregate-version course-id question-id] args]
        (PUT (str "/api/section-test-reveal-worked-out-answer/" section-id "/" student-id "/" course-id "/" question-id)
             {:params {:expected-version section-test-aggregate-version}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel section-id)
              :error-handler (command-error-handler cursor)}))
      "section-test-commands/check-answer"
      (let [[section-id student-id section-test-aggregate-version course-id question-id inputs] args]
        (PUT (str "/api/section-test-check-answer/" section-id "/" student-id "/" course-id "/" question-id)
             {:params {:expected-version section-test-aggregate-version
                       :inputs inputs}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel section-id)
              :error-handler (command-error-handler cursor)}))
      "section-test-commands/next-question"
      (let [[section-id student-id section-test-aggregate-version course-id] args]
        (PUT (str "/api/section-test-next-question/" section-id "/" student-id "/" course-id)
             {:params {:expected-version section-test-aggregate-version}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel section-id)
              :error-handler (command-error-handler cursor)}))

      "section-test-commands/dismiss-modal"
      (let [[section-id student-id section-test-aggregate-version course-id] args]
        (PUT (str "/api/section-test-dismiss-modal/" section-id "/" student-id "/" course-id)
             {:params {:expected-version section-test-aggregate-version}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel section-id)
              :error-handler (command-error-handler cursor)}))

      "entry-quiz-commands/dismiss-nag-screen"
      (let [[course-id student-id] args]
        (PUT (str "/api/entry-quiz-dismiss-nag-screen/" course-id "/" student-id)
             {:format :json
              :handler (command-aggregate-handler cursor notification-channel course-id)
              :error-handler (command-error-handler cursor)}))

      "entry-quiz-commands/init"
      (let [[course-id student-id] args]
        (PUT (str "/api/entry-quiz-init/" course-id "/" student-id)
             {:format :json
              :handler (command-aggregate-handler cursor notification-channel course-id)
              :error-handler (command-error-handler cursor)}))

      "entry-quiz-commands/submit-answer"
      (let [[course-id student-id entry-quiz-aggregate-version inputs] args]
        (PUT (str "/api/entry-quiz-submit-answer/" course-id "/" student-id)
             {:params {:expected-version entry-quiz-aggregate-version
                       :inputs inputs}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel course-id)
              :error-handler (command-error-handler cursor)}))

      "chapter-quiz-commands/start"
      (let [[chapter-id student-id] args
            course-id (get-in @cursor [:static :course-id])
            handler (command-aggregate-handler cursor notification-channel chapter-id)]
        (PUT (str "/api/chapter-quiz-start/" course-id "/" chapter-id "/" student-id)
             {:format :json
              :handler (fn [res]
                         (handler res)
                         (async/put! command-channel ["chapter-quiz-commands/reload" chapter-id student-id]))
              :error-handler (command-error-handler cursor)}))

      "chapter-quiz-commands/stop"
      (let [[chapter-id student-id] args
            course-id (get-in @cursor [:static :course-id])]
        (PUT (str "/api/chapter-quiz-stop/" course-id "/" chapter-id "/" student-id)
             {:format :json
              :handler (command-aggregate-handler cursor notification-channel chapter-id)
              :error-handler (command-error-handler cursor)}))

      "chapter-quiz-commands/reload"
      (let [[chapter-id student-id] args
            course-id (get-in @cursor [:static :course-id])]
        (GET (str "/api/chapter-quiz-replay/" course-id "/" chapter-id "/" student-id)
             {:format :json
              :handler (fn [res]
                         (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
                           (when (seq events)
                             (handle-replay-events cursor chapter-id events aggregate-version))))
              :error-handler basic-error-handler}))


      "chapter-quiz-commands/check-answer"
      (let [[chapter-id student-id chapter-quiz-aggregate-version course-id question-id inputs] args]
        (PUT (str "/api/chapter-quiz-submit-answer/" course-id "/" chapter-id "/" student-id "/" question-id)
             {:params {:expected-version chapter-quiz-aggregate-version
                       :inputs inputs}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel chapter-id)
              :error-handler (command-error-handler cursor)}))

      "chapter-quiz-commands/next-question"
      (let [[chapter-id student-id chapter-quiz-aggregate-version course-id] args]
        (PUT (str "/api/chapter-quiz-dismiss-error-screen/" course-id "/" chapter-id "/" student-id)
             {:params {:expected-version chapter-quiz-aggregate-version}
              :format :json
              :handler (command-aggregate-handler cursor notification-channel chapter-id)
              :error-handler (command-error-handler cursor)}))

      "tracking-commands/navigation"
      (let [[student-id tracking-location] args]
        (PUT (str "/api/tracking/navigation/" student-id)
             {:params {:tracking-location tracking-location}
              :format :json
              :handler ignore-handler
              :error-handler ignore-handler}))
      nil)))

(defn load-data [cursor command]

  (let [[command-type & args] command]
    (condp = command-type
      "data/dashboard"
      (let [[course-id student-id] args]
        (GET (str "/api/course-material/"
                  course-id
                  "/"
                  student-id)
             {:params {}
              :handler (fn [res]
                         (let [course-data (-> (json-edn/json->edn res)
                                               add-chapter-index-mapping
                                               add-forward-section-links)]
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
                                 (if events
                                   (handle-replay-events cursor section-id events aggregate-version)
                                   (handle-replay-events cursor section-id [] -1))))
                    :error-handler basic-error-handler})))))

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
                           (let [section-data (json-edn/json->edn res)]
                             (om/transact! cursor
                                           #(assoc-in %
                                                      [:view :section (:id section-data) :data]
                                                      section-data))))
                :error-handler basic-error-handler})))

      "data/entry-quiz"
      (let [[course-id student-id] args]
        (when-not (get-in @cursor [:view :course-material])
          (load-data cursor
                     ["data/dashboard" course-id student-id]))
        (GET (str "/api/entry-quiz-replay/" course-id "/" student-id)
             {:params {}
              :handler (fn [res]
                         (om/update! cursor
                                     [:view :entry-quiz-replay-done]
                                     true)
                         (let [{:keys [events aggregate-version]} (json-edn/json->edn res)]
                           (when events
                             (handle-replay-events cursor course-id events aggregate-version))))
              :error-handler basic-error-handler}))

      "data/chapter-quiz-question"
      (let [[chapter-id question-id] args
            course-id (get-in @cursor [:static :course-id])]
        (if-let [question-data (get-in @cursor [:view :chapter-quiz chapter-id :questions question-id])]
          nil ;; data already loaded
          (GET (str "/api/course-material/"
                    course-id
                    "/chapter/" chapter-id
                    "/question/" question-id)
               {:params {}
                :handler (fn [res]
                           (let [question-data (json-edn/json->edn res)]
                             (om/transact! cursor
                                           #(assoc-in %
                                                      [:view :chapter-quiz chapter-id :questions question-id]
                                                      question-data))))
                :error-handler basic-error-handler})))

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
        (let [command-channel (om/get-shared owner :command-channel)
              notification-channel (om/get-shared owner :notification-channel)]
          (go (loop []
                (when-let [command (<! command-channel)]
                  (try-command cursor notification-channel command-channel command)
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
