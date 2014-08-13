(ns studyflow.web.entry-quiz
  (:require [goog.dom :as gdom]
            [goog.events :as gevents]
            [goog.events.KeyHandler]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.core :as core]
            [studyflow.web.service :as service]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn entry-quiz-id-for-page []
  (.-value (gdom/getElement "entry-quiz-id")))

(defn init-app-state []
  (atom {:static {:entry-quiz-id (entry-quiz-id-for-page)
                  :student {:id (core/student-id-for-page)
                            :full-name (core/student-full-name-for-page)}
                  :logout-target (core/logout-target-for-page)}
         :view {:questions {}}
         :aggregates {}}))

(defn input-builders
  "mapping from input-name to create react dom element for input type"
  [cursor question current-answers]
  (let [question-id (:id question)]
    (-> {}
        (into (for [mc (:multiple-choice-input-fields question)]
                (let [input-name (:name mc)]
                  [input-name
                   ;; WARNING using dom/ul & dom/li here breaks
                   (apply dom/span #js {:className "mc-list"}
                          (for [choice (map :value (:choices mc))]
                            (let [id (str input-name "-" choice)]
                              (dom/span #js {:className "mc-choice"}
                                        (dom/input #js {:id id
                                                        :type "radio"
                                                        :react-key (str question-id "-" input-name "-" choice)
                                                        :checked (= choice (get current-answers input-name))
                                                        :onChange (fn [event]
                                                                    (om/update!
                                                                     cursor
                                                                     [:view :entry-quiz question-id :answer input-name]
                                                                     choice))}
                                                   (dom/label #js {:htmlFor id}
                                                              choice))))))])))
        (into (for [[li ref] (map list
                                  (:line-input-fields question)
                                  (into ["FOCUSED_INPUT"]
                                        (rest (map :name (:line-input-fields question)))))]
                (let [input-name (:name li)]
                  [input-name
                   (dom/span nil
                             (when-let [prefix (:prefix li)]
                               (str prefix " "))
                             (dom/input
                              #js {:value (get current-answers input-name "")
                                   :react-key (str question-id "-" ref)
                                   :ref ref
                                   :onChange (fn [event]
                                               (om/update!
                                                cursor
                                                [:view :entry-quiz question-id :answer input-name]
                                                (.. event -target -value)))})
                             (when-let [suffix (:suffix li)]
                               (str " " suffix)))]))))))

(defn start-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [entry-quiz-id (get-in cursor [:static :entry-quiz-id])
            student-id (get-in cursor [:static :student :id])
            entry-quiz (get-in cursor [:aggregates entry-quiz-id])
            submit (fn []
                    (prn "handle submit")
                    (async/put! (om/get-shared owner :command-channel)
                                ["student-entry-quiz-commands/init"
                                 entry-quiz-id
                                 student-id]))]
        (om/set-state! owner :submit submit)
        (dom/div nil
                 "Start hier"
                 (dom/div #js {:id "m-button_bar"}
                          (om/build (core/click-once-button
                                     "Start instaptoets"
                                     (fn []
                                       (submit))) cursor)))))
    om/IDidMount
    (did-mount [_]
      (let [key-handler (goog.events.KeyHandler. js/document)]
        (goog.events/listenOnce key-handler
                                goog.events.KeyHandler.EventType.KEY
                                (fn [e]
                                  (when (= (.-keyCode e) 13) ;;enter
                                    (when-let [f (om/get-render-state owner :submit)]
                                      (f)))))))))

(defn to-dashboard-bar []
  (dom/div #js {:id "m-question_bar"}
           (dom/button #js {:className "btn green pull-right"
                            :onClick (fn []
                                       (set! (.-location js/window)
                                             "/"))}
                       "Naar dashboard")))

(defn entry-quiz-panel [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (async/put! (om/get-shared owner :data-channel)
                  ["data/entry-quiz"
                   (get-in cursor [:static :entry-quiz-id])
                   (get-in cursor [:static :student :id])]))
    om/IRender
    (render [_]
      (let [entry-quiz-id (get-in cursor [:static :entry-quiz-id])
            entry-quiz (get-in cursor [:aggregates entry-quiz-id])]
        (dom/div #js {:id "m-entry-quiz"
                      :className "entry_exam_page"}
                 (dom/header #js {:id "m-top_header"}
                             (dom/a #js {:className "home"
                                         :href "/"})
                             (dom/h1 #js {:className "page_heading"}
                                     "Instaptoets") ;; TODO title is not in aggregate
                             (when-let [cnt (:question-progress-count entry-quiz)]
                               (dom/p #js {:className "page_subheading"}
                                      (str "Vraag #: " cnt))) ;; TODO total questions is not in aggregate
                             (dom/article #js {:id "m-section"}
                                          (if-not (get-in cursor [:view :entry-quiz-replay-done])
                                            (dom/div nil "Instaptoets laden"
                                                     (to-dashboard-bar))
                                            (let [status (:status entry-quiz)]
                                              (cond
                                               (nil? status) ;; entry-quiz not yet
                                               ;; started
                                               (om/build start-panel cursor)

                                               (= status :in-progress)
                                               (let [entry-quiz-id (:id entry-quiz)
                                                     entry-quiz-aggregate-version (:aggregate-version entry-quiz)
                                                     student-id (get-in cursor [:static :student :id])
                                                     question (peek (:questions entry-quiz))
                                                     question-id (:id question)
                                                     question-text (:text question)

                                                     current-answers (om/value (get-in cursor [:view :entry-quiz question-id :answer] {}))
                                                     inputs (input-builders cursor question current-answers)
                                                     answering-allowed
                                                     (every? (fn [input-name]
                                                               (seq (get current-answers input-name)))
                                                             (keys inputs))
                                                     submit (fn []
                                                              (when answering-allowed
                                                                (async/put!
                                                                 (om/get-shared owner :command-channel)
                                                                 ["student-entry-quiz-commands/submit-answer"
                                                                  entry-quiz-id
                                                                  student-id
                                                                  entry-quiz-aggregate-version
                                                                  question-id
                                                                  current-answers])))]
                                                 (dom/form #js {:onSubmit (fn []
                                                                            (submit)
                                                                            false)}
                                                           (apply dom/div nil
                                                                  (for [text-or-input (core/split-text-and-inputs question-text
                                                                                                                  (keys inputs))]
                                                                    ;; this wrapper div is
                                                                    ;; required, otherwise the
                                                                    ;; dangerouslySetInnerHTML
                                                                    ;; breaks when mixing html
                                                                    ;; in text and inputs
                                                                    (dom/div #js {:className "dangerous-html-wrap"}
                                                                             (if-let [input (get inputs text-or-input)]
                                                                               input
                                                                               (dom/span #js {:dangerouslySetInnerHTML #js {:__html text-or-input}} nil)))))
                                                           (dom/div #js {:id "m-question_bar"}
                                                                    (om/build (core/click-once-button "Beantwoorden"
                                                                                                      (fn []
                                                                                                        ;; will call onSubmit of form
                                                                                                        nil)
                                                                                                      :enabled answering-allowed)
                                                                              cursor))))
                                               (= status :passed)
                                               (dom/div nil
                                                        (dom/div nil "PASSED Je hebt de instaptoets afgerond. Ga terug naar het dashboard")
                                                        (to-dashboard-bar))
                                               (= status :failed)
                                               (dom/div nil
                                                        (dom/div nil "FAILED Je hebt de instaptoets afgerond. Ga terug naar het dashboard")
                                                        (to-dashboard-bar))))))))))
    om/IDidMount
    (did-mount [_]
      (core/focus-input-box owner))))

(defn widgets [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get-in cursor [:aggregates :failed])
                 (core/modal
                  (dom/h1 nil "Je bent niet meer up-to-date met de server. Herlaad de pagina.")
                  (dom/button #js {:onClick (fn [e]
                                              (.reload js/location true))}
                              "Herlaad de pagina")))
               (om/build entry-quiz-panel cursor)))))

(defn ^:export entry-quiz-page []
  (om/root
   (-> widgets
       service/wrap-service)
   (init-app-state)
   {:target (gdom/getElement "app")
    :tx-listen (fn [tx-report cursor]
                 (service/listen tx-report cursor))
    :shared {:command-channel (async/chan)
             :data-channel (async/chan)}}))
