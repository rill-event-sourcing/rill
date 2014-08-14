(ns studyflow.web.entry-quiz
  (:require [goog.dom :as gdom]
            [goog.events :as gevents]
            [goog.events.KeyHandler]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.core :as core]
            [studyflow.web.om-helpers :refer [raw-html]]
            [studyflow.web.service :as service]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn input-builders
  "mapping from input-name to create react dom element for input type"
  [cursor index question current-answers]
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
                                                      :react-key (str index "-" input-name "-" choice)
                                                      :checked (= choice (get current-answers input-name))
                                                      :onChange (fn [event]
                                                                  (om/update!
                                                                   cursor
                                                                   [:view :entry-quiz index :answer input-name]
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
                                 :react-key (str index "-" ref)
                                 :ref ref
                                 :onChange (fn [event]
                                             (om/update!
                                              cursor
                                              [:view :entry-quiz index :answer input-name]
                                              (.. event -target -value)))})
                           (when-let [suffix (:suffix li)]
                             (str " " suffix)))])))))

(defn start-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [course-id (get-in cursor [:static :course-id])
            student-id (get-in cursor [:static :student :id])
            entry-quiz (get-in cursor [:aggregates course-id])
            submit (fn []
                     (prn "handle submit")
                     (async/put! (om/get-shared owner :command-channel)
                                 ["student-entry-quiz-commands/init"
                                  course-id
                                  student-id]))]
        (om/set-state! owner :submit submit)
        (dom/div nil
                 "Start hier"
                 (dom/div #js {:id "m-button_bar"}
                          (om/build (core/click-once-button
                                     "Naar de eerste vraag"
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

(defn instructions-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [course-id (get-in cursor [:static :course-id])
            student-id (get-in cursor [:static :student :id])
            entry-quiz (get-in cursor [:view :entry-quiz-material])
            submit (fn []
                     (async/put! (om/get-shared owner :command-channel)
                                 ["student-entry-quiz-commands/visit-first-question"
                                  course-id
                                  student-id]))]
        (om/set-state! owner :submit submit)
        (dom/div nil
                 (raw-html (:instructions entry-quiz))
                 (dom/div #js {:id "m-button_bar"}
                          (om/build (core/click-once-button
                                     "Naar de eerste vraag"
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
                       "Naar je Dashboard")))

(defn entry-quiz-panel [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (async/put! (om/get-shared owner :data-channel)
                  ["data/entry-quiz"
                   (get-in cursor [:static :course-id])
                   (get-in cursor [:static :student :id])]))
    om/IRender
    (render [_]
      (let [course-id (get-in cursor [:static :course-id])
            entry-quiz (get-in cursor [:aggregates course-id])
            material (get-in cursor [:view :entry-quiz-material])]
        (dom/div #js {:id "m-entry-quiz"
                      :className "entry_exam_page"}
                 (dom/header #js {:id "m-top_header"}
                             (dom/a #js {:className "home"
                                         :href "/"})
                             (dom/h1 #js {:className "page_heading"}
                                     "Instaptoets") ;; TODO title is not in aggregate
                             (when-let [index (:question-index entry-quiz)]
                               (dom/p #js {:className "page_subheading"}
                                      (str "Vraag " (inc index) " van " (count (:questions material)))))
                             (dom/article #js {:id "m-section"}
                                          (if-not (get-in cursor [:view :entry-quiz-replay-done])
                                            (dom/div nil "Instaptoets laden"
                                                     (to-dashboard-bar))
                                            (case (:status entry-quiz)
                                              nil ; entry-quiz not yet started
                                              (om/build start-panel cursor)

                                              :started
                                              (om/build instructions-panel cursor)

                                              :in-progress
                                              (let [course-id (:id entry-quiz)
                                                    entry-quiz-aggregate-version (:aggregate-version entry-quiz)
                                                    student-id (get-in cursor [:static :student :id])
                                                    index  (:question-index entry-quiz)
                                                    question (get-in material [:questions index])
                                                    question-text (:text question)

                                                    current-answers (om/value (get-in cursor [:view :entry-quiz index :answer] {}))
                                                    inputs (input-builders cursor index question current-answers)
                                                    answering-allowed
                                                    (every? (fn [input-name]
                                                              (seq (get current-answers input-name)))
                                                            (keys inputs))
                                                    submit (fn []
                                                             (when answering-allowed
                                                               (async/put!
                                                                (om/get-shared owner :command-channel)
                                                                ["student-entry-quiz-commands/submit-answer"
                                                                 course-id
                                                                 student-id
                                                                 entry-quiz-aggregate-version
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
                                                                              (raw-html text-or-input)))))
                                                          (dom/div #js {:id "m-question_bar"}
                                                                   (om/build (core/click-once-button "Klaar & naar de volgende vraag"
                                                                                                     (fn []
                                                                                                       ;; will call onSubmit of form
                                                                                                       nil)
                                                                                                     :enabled answering-allowed)
                                                                             cursor))))
                                              :passed
                                              (dom/div nil
                                                       (dom/div nil "PASSED Je hebt de instaptoets afgerond. Ga terug naar het dashboard")
                                                       (to-dashboard-bar))
                                              :failed
                                              (dom/div nil
                                                       (dom/div nil "FAILED Je hebt de instaptoets afgerond. Ga terug naar het dashboard")
                                                       (to-dashboard-bar)))))))))
    om/IDidMount
    (did-mount [_]
      (core/focus-input-box owner))))
