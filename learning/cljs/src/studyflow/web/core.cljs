(ns studyflow.web.core
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.string :as gstring]
            [goog.events :as gevents]
            [goog.events.KeyHandler]
            [goog.Timer :as gtimer]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.service :as service]
            [studyflow.web.history :refer [history-link]]
            [studyflow.web.helpers :refer [modal raw-html split-text-and-inputs render-question]]
            [clojure.walk :as walk]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(set! *print-fn*
      (let [c (some-> js/window .-console)]
        (if (some-> c .-log .-apply)
          (fn [& args]
            (.apply (.-log c) c (into-array args)))
          (fn [& args]))))

(defn course-id-for-page []
  (.-value (gdom/getElement "course-id")))

(defn student-id-for-page []
  (.-value (gdom/getElement "student-id")))

(defn student-full-name-for-page []
  (.-value (gdom/getElement "student-full-name")))

(defn logout-target-for-page []
  (.-value (gdom/getElement "logout-target")))

(defn init-app-state []
  (atom {:static {:course-id (course-id-for-page)
                  :student {:id (student-id-for-page)
                            :full-name (student-full-name-for-page)}
                  :logout-target (logout-target-for-page)}
         :view {:selected-path {:chapter-id nil
                                :section-id nil
                                :main :dashboard
                                :section-tab nil}}
         :aggregates {}}))

(defn show-sidebar [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:id "nav_toggle"
                       :onClick (fn [event]
                                  (om/update!
                                   cursor
                                   [:view :side-navigation :shown]
                                   (not (get-in @cursor [:view :side-navigation :shown]))))}))))

(defn section-explanation-link [cursor chapter section]
  (-> (get-in cursor [:view :selected-path])
      (assoc :chapter-id (:id chapter)
             :section-id (:id section)
             :section-tab :explanation
             :main :learning)
      history-link))

(defn navigation [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :data-channel)
                    ["data/navigation" chapter-id student-id])))
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-tab]} (get-in cursor [:view :selected-path])
            course (get-in cursor [:view :course-material])
            chapter (some (fn [{:keys [id] :as chapter}]
                            (when (= id chapter-id)
                              chapter)) (:chapters course))]
        (dom/div nil
                 (dom/h1 #js {:id "sidenav_chapter_title"} (:title chapter))
                 (apply dom/ul nil
                        (for [{:keys [title]
                               section-id :id
                               :as section} (:sections chapter)]
                          (let [open-section (= section-id
                                                (get-in cursor [:view :selected-path :section-id]))]
                            (apply dom/li #js {:className
                                               (str "section_list_item "
                                                    (when open-section
                                                      "open ")
                                                    (get
                                                     {:finished "finished"
                                                      :in-progress "in_progress"}
                                                     (aggregates/section-test-progress
                                                      (get-in cursor [:aggregates section-id]))
                                                     ""))}
                                   (dom/a #js {:href (section-explanation-link cursor chapter section)
                                               :className "section_link"}
                                          title)
                                   (when open-section
                                     [(dom/a #js {:className (str "section_tab explanation"
                                                                  (when (= section-tab :explanation)
                                                                    " selected"))
                                                  :href (-> (get-in cursor [:view :selected-path])
                                                            (assoc :section-tab :explanation)
                                                            history-link)}
                                             "Uitleg")
                                      (dom/a #js {:className (str "section_tab questions"
                                                                  (when (= section-tab :questions)
                                                                    " selected"))
                                                  :href (-> (get-in cursor [:view :selected-path])
                                                            (assoc :section-tab :questions)
                                                            history-link)}
                                             "Vragen")])))))
                 (dom/div #js {:id "meta_content"}
                          (om/build show-sidebar cursor)))))))

(defn navigation-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            course (get-in cursor [:view :course-material])]
        (dom/nav #js {:id "m-sidenav"}
                 (if-let [chapter (some (fn [{:keys [id] :as chapter}]
                                          (when (= id chapter-id)
                                            chapter)) (:chapters course))]

                   (om/build navigation cursor)
                   (dom/ul nil
                           (dom/li #js {:className "section_list_item"}
                                   (dom/a #js {:className "section_link"}
                                          "Menu laden...")))))))))

(defn question-by-id [cursor section-id question-id]
  (if-let [question (get-in cursor [:view :section section-id :test question-id])]
    question
    (do (om/update! cursor [:view :section section-id :test question-id] nil)
        nil)))

(defn click-once-button [value onclick & {:keys [enabled className]
                                          :or {enabled true}}]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:enabled enabled})
      om/IRender
      (render [_]
        (dom/button #js {:className (str "btn blue pull-right" (when className (str " " className)))
                         :onClick
                         (fn [_]
                           (onclick)
                           (om/set-state-nr! owner :enabled false))
                         :disabled (not (om/get-state owner :enabled))}
                    value)))))

(defn section-input-field
  [cursor owner {:keys [field section]}]
  (let [field-name (:name field)
        correct-answers (:correct-answers field)
        section-id (:id section)]
    (reify
      om/IRender
      (render [_]
        (let [submit (fn []
                       (when-let [f (om/get-state owner :submit)]
                         (f)))
              input-focused (= field-name (get-in cursor [:view :section section-id :input-focused]))
              answer-submitted? (get-in cursor [:view :section section-id :input field-name :answer-submitted?])
              answered-correctly (get-in cursor [:view :section section-id :input field-name :answered-correctly])
              answer-revealed (get-in cursor [:view :section section-id :input field-name :answer-revealed])]
          (dom/span nil
                    (dom/form #js {:className (str "m-inline_input"
                                                   (when answered-correctly
                                                     " correct")
                                                   (when answer-revealed
                                                     " incorrect"))
                                   :onBlur (fn [event]
                                              (om/update!
                                               cursor
                                               [:view :section section-id :input-focused]
                                               nil))
                                   :onFocus (fn [event]
                                              (om/update!
                                               cursor
                                               [:view :section section-id :input-focused]
                                               field-name))
                                   :onSubmit (fn [e]
                                               (submit)
                                               false)}
                              (dom/input
                               #js {:className "inline_input"
                                    :react-key (:name field)
                                    :ref (:name field)
                                    :value (get-in cursor [:view :section section-id :input field-name :given-answer])
                                    :disabled (get-in cursor [:view :section section-id :input field-name :input-disabled])

                                    :onChange (fn [event]
                                                (om/update!
                                                 cursor
                                                 [:view :section section-id :input field-name :answer-submitted?]
                                                 false)
                                                (om/update!
                                                 cursor
                                                 [:view :section section-id :input field-name :given-answer]
                                                 (.. event -target -value)))})
                              (when input-focused
                                (dom/div #js {:className "inline_input_tooltip"}
                                         (when (and answer-revealed
                                                    (not answer-submitted?)
                                                    (not answered-correctly))
                                           (dom/p #js {:className "answer"}
                                                  (first correct-answers)))
                                         (when (and input-focused
                                                    (not answer-submitted?)
                                                    (not answered-correctly))
                                           (om/set-state-nr! owner :submit
                                                             (fn []
                                                               (om/update!
                                                                cursor
                                                                [:view :section section-id :input field-name :answered-correctly]
                                                                (contains?
                                                                 (set @correct-answers)
                                                                 (get-in @cursor [:view :section section-id :input field-name :given-answer]))
                                                                :answered)))
                                           (dom/input
                                            #js {:type "submit"
                                                 :className "inline_input_button"
                                                 :value "Nakijken"
                                                 :onClick (fn [event]
                                                            (om/update!
                                                             cursor
                                                             [:view :section section-id :input field-name :answer-submitted?]
                                                             true)
                                                            (submit)
                                                            (.focus (.getDOMNode (aget (.-refs owner) field-name))))}))
                                         (if answered-correctly
                                           (do
                                             (om/set-state-nr! owner :submit (fn []))
                                             (om/update!
                                              cursor
                                              [:view :section section-id :input field-name :input-disabled]
                                              true)
                                             (dom/p #js {:className "correct"} "Goed!"))
                                           (when (and (not answer-revealed)
                                                      answer-submitted?
                                                      (false? answered-correctly))
                                             (dom/p #js {:className "incorrect"} "Fout! :(")))
                                         (when (and (not answer-revealed)
                                                    answer-submitted?
                                                    (false? answered-correctly))
                                           (dom/input
                                            #js {:className "inline_input_button"
                                                 :type "submit"
                                                 :value "Toon antwoord"
                                                 :onClick (fn [event]
                                                            (om/update!
                                                             cursor
                                                             [:view :section section-id :input field-name :answer-submitted?]
                                                             true)
                                                            (om/update!
                                                             cursor
                                                             [:view :section section-id :input field-name :answer-revealed]
                                                             true))})))))))))))

(defn input-builders-subsection
  "mapping from input-name to create react dom element for input type"
  [cursor section]
  (-> {}
      (into (for [li (:line-input-fields section)]
              [(:name li) (om/build section-input-field cursor {:opts {:field li :section section}})]))))

(defn section-explanation [section owner]
  (reify
    om/IRender
    (render [_]
      (let [subsections (get section :subsections)
            inputs (input-builders-subsection section section)]
        (println [:inputs! inputs])
        (apply dom/article #js {:id "m-section"}
               #_(dom/nav #js {:id "m-minimap"}
                          (apply dom/ul nil
                                 (for [{:keys [title id]
                                        :as subsection} subsections]
                                   (dom/li nil title))))
               (map (fn [{:keys [title text id] :as subsection}]
                      (dom/section #js {:className "m-subsection"}
                                   (apply dom/div nil
                                          (for [text-or-input (split-text-and-inputs text
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


                                   ))

                    subsections))))))

(defn section-explanation-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            section (get-in cursor [:view :section section-id :data])]
        (if section
          (om/build section-explanation section)
          (dom/article #js {:id "m-section"}
                       "Uitleg laden..."))))))

(defn streak-box [streak owner]
  (reify
    om/IRender
    (render [_]
      (let [streak
            (if (< (count streak) 5)
              (take 5 (concat (reverse streak) (repeat 5 [nil :inactive])))
              (reverse streak))]
        (apply dom/div #js {:id "m-path"}
               (map-indexed
                (fn [idx [question-id result]]
                  (dom/span #js {:className (str
                                             "goal "
                                             (if (< idx 5)
                                               ""
                                               "inactive ")
                                             (condp = result
                                               :correct "correct"
                                               :incorrect "incorrect"
                                               :revealed "hint" ;; warning this is currently for worked out answer
                                               :open ""
                                               :inactive "inactive"))}))
                streak))))))

(defn input-builders
  "mapping from input-name to create react dom element for input type"
  [cursor section-id question-id question-index question-data current-answers submitted-answers answer-correct]
  (let [disabled answer-correct
        current-answers (if disabled
                          (zipmap (map name (keys submitted-answers))
                                  (vals submitted-answers))
                          current-answers)]
    (-> {}
        (into (for [mc (:multiple-choice-input-fields question-data)]
                (let [input-name (:name mc)]
                  [input-name
                   ;; WARNING using dom/ul & dom/li here breaks
                   (apply dom/span #js {:className "mc-list"}
                          (for [choice (map :value (:choices mc))]
                            (let [id (str input-name "-" choice)]
                              (dom/span #js {:className "mc-choice"}
                                        (dom/input #js {:id id
                                                        :react-key (str question-id "-" question-index "-" input-name "-" choice)
                                                        :type "radio"
                                                        :checked (= choice (get current-answers input-name))
                                                        :disabled disabled
                                                        :onChange (fn [event]
                                                                    (om/update!
                                                                     cursor
                                                                     [:view :section section-id :test :questions [question-id question-index] :answer input-name]
                                                                     choice))}
                                                   (dom/label #js {:htmlFor id}
                                                              (raw-html choice)))))))])))
        (into (for [[li ref] (map list
                                  (:line-input-fields question-data)
                                  (into ["FOCUSED_INPUT"]
                                        (rest (map :name (:line-input-fields question-data)))))]
                (let [input-name (:name li)]
                  [input-name
                   (dom/span nil
                             (when-let [prefix (:prefix li)]
                               (str prefix " "))
                             (dom/input
                              #js {:value (get current-answers input-name "")
                                   :react-key (str question-id "-" question-index "-" ref)
                                   :ref ref
                                   :disabled disabled
                                   :onChange (fn [event]
                                               (om/update!
                                                cursor
                                                [:view :section section-id :test :questions [question-id question-index] :answer input-name]
                                                (.. event -target -value)))})
                             (when-let [suffix (:suffix li)]
                               (str " " suffix)))]))))))

(defn focus-input-box [owner]
  ;; we always call this, even when there's no element called
  ;; "FOCUSED_INPUT". om/get-node can't handle that case
  (when-let [refs (.-refs owner)]
    ;; need to set the focus on a non disabled field for firefox key handling
    (if-let [button-ref (aget refs "FOCUSED_BUTTON")]
      (when-let [button (.getDOMNode button-ref)]
        (.focus button))
      (when-let [input-ref (aget refs "FOCUSED_INPUT")]
        (when-let [input-field (.getDOMNode input-ref)]
          (when (= "" (.-value input-field))
            (.focus input-field)))))))

(defn tool-box
  [tools]
  (let [tool-names {"pen_and_paper" "Pen & Papier"
                    "calculator" "Rekenmachine"}]
    (apply dom/div #js {:id "toolbox"}
           (map (fn [tool]
                  (dom/div #js {:className (str "tool " tool)}
                           (dom/div #js {:className "m-tooltip"} (get tool-names tool) )))
                tools))))

(defn reveal-answer-button [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [revealed-answer question-id question-data section-id student-id section-test-aggregate-version course-id]} cursor
            can-reveal-answer (get question-data :has-worked-out-answer)]
        (if can-reveal-answer
          (dom/button #js {:className "btn light_blue icon hint"
                           :disabled
                           (boolean revealed-answer)
                           :onClick
                           (fn [e]
                             (async/put! (om/get-shared owner :command-channel)
                                         ["section-test-commands/reveal-worked-out-answer"
                                          section-id
                                          student-id
                                          section-test-aggregate-version
                                          course-id
                                          question-id]))}
                      "Toon antwoord")
          (dom/span nil nil))))))

(def key-listener (atom nil)) ;; should go into either cursor or local state

(defn question-panel [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [notification-channel (om/get-shared owner :notification-channel)]
        (go (loop []
              (when-let [event (<! notification-channel)]
                (condp = (:type event)
                  "studyflow.learning.section-test.events/Finished"
                  (om/update! cursor
                              [:view :progress-modal]
                              :launchable)
                  "studyflow.web.ui/FinishedModal"
                  (om/update! cursor
                              [:view :progress-modal]
                              :show-finish-modal)
                  "studyflow.learning.section-test.events/StreakCompleted"
                  (om/update! cursor
                              [:view :progress-modal]
                              :show-streak-completed-modal)
                  "studyflow.learning.section-test.events/QuestionAnsweredIncorrectly"
                  (do (om/update! cursor
                                  [:view :shake-class]
                                  "shake")
                      (gtimer/callOnce
                       (fn []
                         (om/update! cursor
                                     [:view :shake-class]
                                     nil))
                       300))
                  nil)
                (recur))))))
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            section-test (get-in cursor [:aggregates section-id])
            questions (:questions section-test)
            question (peek questions)
            question-id (:question-id question)
            question-data (question-by-id cursor section-id question-id)
            question-index (:question-index question)
            current-answers (->> (get-in cursor [:view :section section-id :test :questions [question-id question-index] :answer] {})
                                 ;; deref permanently
                                 (into {}))
            answer-correct (when (contains? question :correct)
                             (:correct question))
            finished-last-action (aggregates/finished-last-action section-test)
            progress-modal (get-in cursor [:view :progress-modal])
            course-id (get-in cursor [:static :course-id])
            section-test-aggregate-version (:aggregate-version section-test)
            inputs (input-builders cursor section-id question-id question-index question-data current-answers (:inputs question) answer-correct)
            answering-allowed (and (not answer-correct)
                                   (every? (fn [input-name]
                                             (seq (get current-answers input-name)))
                                           (keys inputs)))
            _ (om/set-state-nr! owner :submit
                                (fn []
                                  (when answering-allowed
                                    (async/put!
                                     (om/get-shared owner :command-channel)
                                     ["section-test-commands/check-answer"
                                      section-id
                                      student-id
                                      section-test-aggregate-version
                                      course-id
                                      question-id
                                      current-answers]))
                                  (when answer-correct
                                    (when (and finished-last-action
                                               (= progress-modal :launchable))
                                      (let [notification-channel (om/get-shared owner :notification-channel)]
                                        (async/put! notification-channel {:type "studyflow.web.ui/FinishedModal"})))
                                    (when (or (not finished-last-action)
                                              (or (not progress-modal)
                                                  (= progress-modal :dismissed)))
                                      (async/put! (om/get-shared owner :command-channel)
                                                  ["section-test-commands/next-question"
                                                   section-id
                                                   student-id
                                                   section-test-aggregate-version
                                                   course-id])))
                                  (when (or (= progress-modal :show-finish-modal)
                                            (= progress-modal :show-streak-completed-modal))
                                    (do
                                      (om/update! cursor
                                                  [:view :selected-path]
                                                  (-> (get-in @cursor [:view :selected-path])
                                                      (merge (get-in @cursor [:view :course-material :forward-section-links
                                                                              {:chapter-id chapter-id :section-id section-id}]))))
                                      (om/update! cursor
                                                  [:view :progress-modal]
                                                  :dismissed)))
                                  (om/set-state-nr! owner :submit nil)))
            submit (fn []
                     (when-let [f (om/get-state owner :submit)]
                       (f)))
            revealed-answer (get question :worked-out-answer)]
        (dom/div nil (let [progress-modal (get-in cursor [:view :progress-modal])]
                       (condp = progress-modal
                         :show-finish-modal
                         (modal (dom/span nil
                                          (raw-html "<h1>Yes! Je hebt 5 vragen achter elkaar goed!</h1><img src=\"https://s3-eu-west-1.amazonaws.com/studyflow-assets/images/206.gif\"><p>Deze paragraaf is nu klaar. Ga verder naar de volgende paragraaf (of blijf nog even oefenen).</p>"))
                                (dom/button #js {:onClick (fn [e]
                                                            (submit))}
                                            "Volgende paragraaf")
                                (dom/a #js {:href ""
                                            :onClick (fn [e]
                                                       (om/update! cursor
                                                                   [:view :progress-modal]
                                                                   :dismissed)
                                                       (async/put! (om/get-shared owner :command-channel)
                                                                   ["section-test-commands/next-question"
                                                                    section-id
                                                                    student-id
                                                                    section-test-aggregate-version
                                                                    course-id])
                                                       false)}
                                       "Blijven oefenen"))
                         :show-streak-completed-modal
                         (modal (dom/span nil
                                          (raw-html "<h1>Hoppa! Weer goed!</h1><img src=\"https://s3-eu-west-1.amazonaws.com/studyflow-assets/images/184.gif\"><p>Je hebt deze paragraaf nog een keer voltooid.<br>We denken dat je hem nu wel snapt :).</p>"))
                                (dom/button #js {:onClick (fn [e]
                                                            (submit))}
                                            "Volgende paragraaf"))
                         nil))
                 (dom/article #js {:id "m-section"}
                              (render-question (:text question-data) inputs)
                              (when revealed-answer
                                (dom/div #js {:dangerouslySetInnerHTML #js {:__html revealed-answer}} nil)))
                 (dom/div #js {:id "m-question_bar"}
                          (tool-box (:tools question-data))
                          (if answer-correct
                            (if (and finished-last-action
                                     (= progress-modal :launchable))
                              (om/build (click-once-button "Goed! Voltooi paragraaf"
                                                           (fn []
                                                             (submit))) cursor)
                              ;; this doesn't have the disabled handling
                              ;; as all the click-once-buttons because
                              ;; we need the ref for set-focus
                              (dom/button #js {:className "btn blue pull-right"
                                               :ref "FOCUSED_BUTTON"
                                               :onClick
                                               (fn []
                                                 (submit))}
                                          "Goed! Volgende vraag"))
                            (om/build (click-once-button "Nakijken"
                                                         (fn []
                                                           (submit))
                                                         :enabled answering-allowed
                                                         :className (get-in cursor [:view :shake-class]))
                                      cursor))
                          (om/build reveal-answer-button
                                    {:revealed-answer revealed-answer
                                     :question-id question-id
                                     :question-data question-data

                                     :section-id section-id
                                     :student-id student-id
                                     :section-test-aggregate-version section-test-aggregate-version
                                     :course-id course-id})))))
    om/IDidUpdate
    (did-update [_ _ _]
      (focus-input-box owner))
    om/IDidMount
    (did-mount [_]
      (focus-input-box owner)
      (let [key-handler (goog.events.KeyHandler. js/document)]
        (when-let [key @key-listener]
          (goog.events/unlistenByKey key))
        (->> (goog.events/listen key-handler
                                 goog.events.KeyHandler.EventType.KEY
                                 (fn [e]
                                   (when (= (.-keyCode e) 13) ;;enter
                                     (when-let [f (om/get-state owner :submit)]
                                       (f)))))
             (reset! key-listener))))))

(defn section-test-loading [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [{:keys [section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :command-channel)
                    ["section-test-commands/init-when-nil"
                     section-id
                     student-id])))
    om/IRender
    (render [_]
      (dom/article #js {:id "m-section"} "Vragen aan het laden..."))))

(defn section-test [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])]
        (if-let [section-test (get-in cursor [:aggregates section-id])]
          (let [questions (:questions section-test)
                question (peek questions)
                question-id (:question-id question)]
            (if-let [question-data (question-by-id cursor section-id question-id)]
              (om/build question-panel cursor)
              (dom/article #js {:id "m-section"} "Vraag laden")))
          (om/build section-test-loading cursor))))))

(defn path-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [section-id section-tab]} (get-in cursor [:view :selected-path])]
        (if (= section-tab :questions)
          (let [student-id (get-in cursor [:static :student :id])
                section-test (get-in cursor [:aggregates section-id])]
            (om/build streak-box (:streak section-test)))
          (dom/div #js {:id "m-path"}))))))

(defn section-panel [cursor owner]
  (let [load-data (fn []
                    (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])]
                      (when (and chapter-id section-id)
                        (async/put! (om/get-shared owner :data-channel)
                                    ["data/section-explanation" chapter-id section-id]))))]
    (reify
      om/IRender
      (render [_]
        (let [{:keys [chapter-id section-id section-tab]} (get-in cursor [:view :selected-path])
              selected-path (get-in cursor [:view :selected-path])]
          (load-data) ;; hacky should go through will-mount?/will-update?
          (dom/section #js {:id "main"}
                       (if (= section-tab :explanation)
                         (if section-id
                           (om/build section-explanation-panel cursor)
                           (dom/article #js {:id "m-section"}
                                        "Maak een keuze uit het menu"))
                         (om/build section-test cursor))
                       (om/build path-panel cursor)))))))

(defn finished? [element]
  (if-not (= (:status element) "finished")
    element))

(defn first-non-completed-chapter [course]
  (some finished? (:chapters course)))

(defn first-non-completed-section [chapter]
  (some finished? (:sections chapter)))

(defn recommended-action [cursor]
  (let [course (get-in cursor [:view :course-material])
        entry-quiz (:entry-quiz course)]
    (if (not (contains? #{"passed" "failed"} (:status entry-quiz)))
      {:title "Instaptoets"
       :link (history-link {:main :entry-quiz})
       :id (:id entry-quiz)}
      (let [chapter (first-non-completed-chapter course)
            section (first-non-completed-section chapter)]
        {:title (:title section)
         :id (:id section)
         :link (section-explanation-link cursor chapter section)} ))))

(defn sections-navigation [cursor chapter]
  (apply dom/ol #js {:id "section_list"}
         (let [recommended-id (:id (recommended-action cursor))]
           (for [{:keys [title status]
                  section-id :id
                  :as section} (:sections chapter)]
             (let [section-status (get {"finished" "finished"
                                        "in-progress" "in_progress"} status "")
                   section-link (section-explanation-link cursor chapter section)]
               (dom/li #js {:data-id section-id
                            :className (str "section_list_item " section-status
                                            (when (= recommended-id section-id) " recommended")) }
                       (dom/a #js {:href section-link
                                   :className (str "section_link "
                                                   section-status)}
                              title)
                       (dom/a #js {:className "btn blue chapter_nav_btn"
                                   :href section-link} "Start")))))))

(defn chapter-navigation [cursor selected-chapter-id course chapter]
  (let [selected? (= selected-chapter-id (:id chapter))]
    (dom/li #js {:className (str "chapter_list_item"
                                 (when (= (:status chapter) "finished") " finished")
                                 (when selected? " open"))}
            (dom/a #js {:data-id (:id course)
                        :className "chapter_title"
                        :href (-> (get-in cursor [:view :selected-path])
                                  (assoc :chapter-id (:id chapter)
                                         :section-id nil
                                         :main :dashboard)
                                  history-link)}
                   (:title chapter))
            (when selected?
              (sections-navigation cursor chapter)))))

(defn dashboard-navigation [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :data-channel)
                    ["data/navigation" chapter-id student-id])))
    om/IRender
    (render [_]
      (let [course (get-in cursor [:view :course-material])
            chapter-id (or (get-in cursor [:view :selected-path :chapter-id]) (:id (first (:chapters course))))]

        (if course
          (dom/div nil
                   (dom/nav #js {:id "m-dashboard_chapter_nav"}
                            (dom/h1 #js {:className "chapter_nav_title"} "Mijn Leerroute")
                            (apply dom/ol #js {:className "chapter_list"}
                                   (let [{:keys [name status]
                                          entry-quiz-id :id
                                          :as entry-quiz} (get-in cursor [:view :course-material :entry-quiz])
                                          status (keyword status)]
                                     (when (not (#{:passed :failed} status))
                                       (dom/li #js {:className "chapter_list_item"}
                                               (dom/a #js {:className "chapter_title"
                                                           :href (history-link {:main :entry-quiz})}
                                                      "Instaptoets"))))
                                   (map (partial chapter-navigation cursor chapter-id course)
                                        (:chapters course))))
                   (dom/div #js {:id "m-path"}))
          (dom/h2 nil "Hoofdstukken laden..."))))))

(defn dashboard-top-header
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/header #js {:id "m-top_header"}
                  (dom/h1 #js {:id "logo"} (:name (get-in cursor [:view :course-material])))
                  (dom/a #js {:id "help" :href "#"})
                  (dom/a #js {:id "settings" :href "#"})
                  (dom/form #js {:method "POST"
                                 :action (get-in cursor [:static :logout-target])
                                 :id "logout-form"}
                            (dom/input #js {:type "hidden"
                                            :name "_method"
                                            :value "DELETE"})
                            (dom/button #js {:type "submit"}
                                        "Uitloggen"))))))

(defn dashboard-sidenav
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/aside #js {:id "m-sidenav"}
                 (dom/div #js {:id "student_info"} (get-in cursor [:static :student :full-name]))
                 (dom/div #js {:id "recommended_action"}
                          (let [{:keys [title link]} (recommended-action cursor)]
                            (dom/div nil
                                     (dom/span nil "Ga verder met:")
                                     (dom/p #js {:id "recommended_title"} title)
                                     (dom/a #js {:id "recommended_button"
                                                 :className "btn big yellow" :href link} "Start"))))
                 (dom/div #js {:id "support_info"})))))

(defn dashboard [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (async/put! (om/get-shared owner :data-channel)
                  ["data/dashboard" (get-in cursor [:static :course-id]) (get-in cursor  [:static :student :id])]))
    om/IRender
    (render [_]
      (dom/div #js {:id "dashboard_page"}
               (om/build dashboard-top-header cursor)
               (om/build dashboard-sidenav cursor)
               (dom/section #js {:id "main"}
                            (om/build dashboard-navigation cursor))))))

(defn page-header [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id section-tab]} (get-in cursor [:view :selected-path])
            course (get-in cursor [:view :course-material])
            chapter (some (fn [{:keys [id] :as chapter}]
                            (when (= id chapter-id)
                              chapter)) (:chapters course))
            section (some (fn [{:keys [id] :as section}]
                            (when (= id section-id)
                              section)) (:sections chapter))]
        (dom/header #js {:id "m-top_header"}
                    (dom/a #js {:id "home"
                                :href  (-> (get-in cursor [:view :selected-path])
                                           (assoc :main :dashboard)
                                           history-link)})
                    (dom/h1 #js {:id "page_heading"}
                            (:title section)))))))
