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
            [studyflow.web.chapter-quiz :as chapter-quiz]
            [studyflow.web.service :as service]
            [studyflow.web.history :refer [history-link]]
            [studyflow.web.helpers :refer [input-builders tool-box modal raw-html tag-tree-to-om focus-input-box section-explanation-link] :as helpers]
            [studyflow.web.recommended-action :refer [recommended-action]]
            [clojure.walk :as walk]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:dynamic *println-to-console* false)

(set! *print-newline* false)
(set! *print-fn*
      (fn [& args]
        (when *println-to-console*
          (.apply (.-log js/console) js/console (into-array args)))))

(defn course-id-for-page []
  (.-value (gdom/getElement "course-id")))

(defn student-id-for-page []
  (.-value (gdom/getElement "student-id")))

(defn student-full-name-for-page []
  (.-value (gdom/getElement "student-full-name")))

(defn logout-target-for-page []
  (.-value (gdom/getElement "logout-target")))

(defn teaching-url-for-page []
  (let [url (.-value (gdom/getElement "teaching-target"))]
    (when (seq url)
      url)))

(defn init-app-state []
  (atom {:static {:course-id (course-id-for-page)
                  :student {:id (student-id-for-page)
                            :full-name (student-full-name-for-page)}
                  :logout-target (logout-target-for-page)
                  :teaching-url (teaching-url-for-page)}
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
                        (concat
                         (for [{:keys [title]
                                section-id :id
                                :as section} (:sections chapter)]
                           (let [open-section (= section-id
                                                 (get-in cursor [:view :selected-path :section-id]))]
                             (dom/li #js {:className
                                          (str "section_list_item "
                                               (when open-section
                                                 "open ")
                                               (get
                                                {:finished "finished"
                                                 :stuck "stumbling_block"
                                                 :in-progress "in_progress"}
                                                (aggregates/section-test-progress
                                                 (get-in cursor [:aggregates section-id]))
                                                ""))}
                                     (dom/a #js {:href (section-explanation-link cursor chapter section)
                                                 :className "section_link"}
                                            title))))
                         [(chapter-quiz/chapter-quiz-navigation-button cursor (:chapter-quiz chapter) chapter-id)]))
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
                           (helpers/ipad-reset-header)
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
        (let [focus-input-field (fn []
                                  (.focus (.getDOMNode (aget (.-refs owner) field-name))))
              submit (fn []
                       (when-let [f (om/get-state owner :submit)]
                         (f)))
              input-focused (= field-name (get-in cursor [:view :section section-id :input-focused]))
              answer-submitted? (get-in cursor [:view :section section-id :input field-name :answer-submitted?])
              answered-correctly? (get-in cursor [:view :section section-id :input field-name :answered-correctly?])
              answer-revealed (get-in cursor [:view :section section-id :input field-name :answer-revealed])
              input-classes (str ""
                                 (when (:prefix field) "has-prefix ")
                                 (when (:suffix field) "has-suffix "))
              input-options (case (:style field)
                              "small" {:class (str input-classes "small-input") :length 5}
                              "exponent" {:class (str input-classes "exponent-input") :length 3}
                              {:class (str input-classes "big-input")})]
          (dom/span nil
                    (when-let [prefix (:prefix field)]
                      (dom/span #js {:className "prefix"} prefix))
                    (dom/form
                     #js {:className (str "m-inline_input"
                                          (when (and answer-submitted? answered-correctly?)
                                            " correct")
                                          (when (and answer-submitted? (false? answered-correctly?))
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
                      #js {:className (str "inline_input " (:class input-options))
                           :maxLength (:length input-options)
                           :placeholder "................."
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
                                        [:view :section section-id :input field-name :answered-correctly?]
                                        false)
                                       (om/update!
                                        cursor
                                        [:view :section section-id :input field-name :given-answer]
                                        (.. event -target -value)))})
                     (when input-focused
                       (dom/div #js {:className "inline_input_tooltip"}
                                (when (and answer-revealed
                                           answer-submitted?
                                           (not answered-correctly?))
                                  (dom/span #js {:className "answer"}
                                            (first correct-answers)))
                                (when (and input-focused
                                           (not answer-submitted?)
                                           (not answered-correctly?))
                                  (om/set-state-nr! owner :submit
                                                    (fn []
                                                      (om/update!
                                                       cursor
                                                       [:view :section section-id :input field-name :answered-correctly?]
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
                                                   (focus-input-field)
                                                   false)}))
                                (if answered-correctly?
                                  (do
                                    (om/set-state-nr! owner :submit (fn []))
                                    (dom/span #js {:className "correct"} "Goed!"))
                                  (when (and (not answer-revealed)
                                             answer-submitted?
                                             (false? answered-correctly?))
                                    (dom/span #js {:className "incorrect"} "Fout! :(")))
                                (when (and (not answer-revealed)
                                           answer-submitted?
                                           (false? answered-correctly?))
                                  (dom/input
                                   #js {:className "inline_input_button"
                                        :type "submit"
                                        :value "Toon antwoord"
                                        :onClick (fn [event]
                                                   (om/update!
                                                    cursor
                                                    [:view :section section-id :input field-name :answer-submitted?]
                                                    true)
                                                   (focus-input-field)
                                                   (om/update!
                                                    cursor
                                                    [:view :section section-id :input field-name :answer-revealed]
                                                    true)
                                                   false)})))))
                    (when-let [suffix (:suffix field)]
                      (dom/span #js {:className "suffix"} suffix))))))))

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
               (map (fn [{:keys [title tag-tree id] :as subsection}]
                      (dom/section #js {:className "m-subsection"}
                                   (tag-tree-to-om tag-tree inputs)))
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
                             (helpers/ipad-reset-header)
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

(defn watch-notifications!
  [notification-channel cursor]
  (go (loop []
        (when-let [event (<! notification-channel)]
          (case (:type event)
            "studyflow.learning.section-test.events/Finished"
            (om/update! cursor
                        [:view :progress-modal]
                        :launchable)

            "studyflow.web.ui/FinishedModal"
            (om/update! cursor
                        [:view :progress-modal]
                        :show-finish-modal)

            "studyflow.learning.section-test.events/Stuck"
            (om/update! cursor
                        [:view :progress-modal]
                        :show-stuck-modal)

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

            "studyflow.learning.chapter-quiz.events/QuestionAssigned"
            (om/update! cursor [:view :chapter-quiz (:chapter-id event) :test :questions] {})

            "studyflow.learning.chapter-quiz.events/Stopped"
            (set! (.-location js/window)
                  (history-link {:main :dashboard
                                 :chapter-id (:chapter-id event)
                                 :section-id nil}))
            nil)
          (recur)))))

(defn question-panel [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      )
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
            progress-modal (get-in cursor [:view :progress-modal])
            complete-again-section-gif "https://assets.studyflow.nl/learning/184.gif"
            stumbling-gif "https://assets.studyflow.nl/learning/187.gif"
            finish-section-gif (rand-nth ["https://assets.studyflow.nl/learning/206.gif"
                                          "https://assets.studyflow.nl/learning/haters.gif"
                                          "https://assets.studyflow.nl/learning/helping-dogs.gif"
                                          "https://assets.studyflow.nl/learning/sewing.gif"
                                          "https://assets.studyflow.nl/learning/milk.gif"])
            explanation-link (-> (get-in cursor [:view :selected-path])
                                 (assoc :section-tab :explanation)
                                 history-link)
            course-id (get-in cursor [:static :course-id])
            section-test-aggregate-version (:aggregate-version section-test)
            submitted-answers (:inputs question)
            current-answers (if answer-correct
                              (zipmap (map name (keys submitted-answers))
                                      (vals submitted-answers))
                              current-answers)
            inputs (input-builders cursor question-id question-data current-answers (not answer-correct)
                                   [:view :section section-id :test :questions [question-id question-index] :answer])
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
                                  (let [progress-modal (get-in @cursor [:view :progress-modal])]
                                    (when answer-correct
                                      (if (= progress-modal :launchable)
                                        (let [notification-channel (om/get-shared owner :notification-channel)]
                                          (async/put! notification-channel {:type "studyflow.web.ui/FinishedModal"}))
                                        ;; not= progress-modal :launchable
                                        (async/put! (om/get-shared owner :command-channel)
                                                    ["section-test-commands/next-question"
                                                     section-id
                                                     student-id
                                                     section-test-aggregate-version
                                                     course-id]))))
                                  (when (or (= progress-modal :show-finish-modal)
                                            (= progress-modal :show-streak-completed-modal))
                                    (do
                                      (om/update! cursor
                                                  [:view :selected-path]
                                                  (let [cursor @cursor]
                                                    (-> (get-in cursor [:view :selected-path])
                                                        (merge (get-in cursor [:view :course-material :forward-section-links
                                                                               {:chapter-id chapter-id :section-id section-id}])))))
                                      (om/update! cursor
                                                  [:view :progress-modal]
                                                  :dismissed)))
                                  (om/set-state-nr! owner :submit nil)))
            submit (fn []
                     (when-let [f (om/get-state owner :submit)]
                       (f)))
            revealed-answer (get question :worked-out-answer)]

        (dom/div nil (condp = progress-modal
                       :show-finish-modal
                       (modal (dom/span nil
                                        (dom/h1 nil "Yes! Je hebt 5 vragen achter elkaar goed!")
                                        (dom/img #js {:src finish-section-gif})
                                        (dom/p nil "Deze paragraaf is nu klaar. Ga verder naar de volgende paragraaf (of blijf nog even oefenen)." ))
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
                       :show-stuck-modal
                       (modal (dom/span nil
                                        (dom/h1 #js {:className "stumbling_block"} "Oeps! deze is moeilijk")
                                        (dom/img #js {:src stumbling-gif})
                                        (dom/p nil "We raden je aan om de uitleg nog een keer te lezen." (dom/br nil) "Dan worden de vragen makkelijker!"))

                              (dom/button #js {:onClick
                                               (fn [e]
                                                 (om/update! cursor
                                                             [:view :progress-modal]
                                                             :dismissed)
                                                 (js/window.location.assign explanation-link))}
                                          "Uitleg lezen")
                              nil)

                       :show-streak-completed-modal
                       (modal (dom/span nil
                                        (dom/h1 nil "Hoppa! Weer goed!")
                                        (dom/img #js {:src complete-again-section-gif})
                                        (dom/p nil "Je hebt deze paragraaf nog een keer voltooid." (dom/br nil) "We denken dat je hem nu wel snapt :)."))
                              (dom/button #js {:onClick (fn [e]
                                                          (submit))}
                                          "Volgende paragraaf"))
                       nil)

                 (dom/article #js {:id "m-section"}
                              (tag-tree-to-om (:tag-tree question-data) inputs)
                              (when revealed-answer
                                (dom/div #js {:className "wrap-dangerous-html m-worked-out-answer"}
                                         (dom/div #js {:dangerouslySetInnerHTML #js {:__html revealed-answer}} nil))))
                 (dom/div #js {:id "m-question_bar"}
                          (tool-box (:tools question-data))
                          (if answer-correct
                            ;; this doesn't have the disabled handling
                            ;; as all the click-once-buttons because
                            ;; we need the ref for set-focus
                            (dom/button #js {:className "btn blue pull-right"
                                             :ref "FOCUSED_BUTTON"
                                             :onClick
                                             (fn []
                                               (submit))}
                                        (if (= progress-modal :launchable)
                                          "Goed! Voltooi paragraaf"
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
      (helpers/ipad-fix-scroll-after-switching)
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
      (helpers/ipad-fix-scroll-after-switching)
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

(defn sections-navigation [cursor chapter]
  (apply dom/ol #js {:id "section_list"}
         (let [chapter-id (:id chapter)
               rcm-action (recommended-action cursor)
               recommended-id (:id rcm-action)]
           (concat
            (for [{:keys [title status]
                   section-id :id
                   :as section} (:sections chapter)]
              (let [section-status (get {"finished" "finished"
                                         "stuck" "stumbling_block"
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
                                    :href section-link} "Start"))))
            [(chapter-quiz/chapter-quiz-navigation-button cursor (:chapter-quiz chapter) chapter-id)]))))

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
                  (when-let [url (get-in cursor [:static :teaching-url])]
                    (dom/a #js {:id "teaching" :href url} "Docent omgeving"))
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
                    (dom/ul #js {:className "section-toggle"}
                            (dom/li nil
                                    (dom/a #js {:className (str "section-toggle-link explanation-link"
                                                                (when (= section-tab :explanation)
                                                                  " selected"))
                                                :href (-> (get-in cursor [:view :selected-path])
                                                          (assoc :section-tab :explanation)
                                                          history-link)}
                                           "Uitleg"))
                            (dom/li nil
                                    (dom/a #js {:className (str "section-toggle-link questions-link"
                                                                (when (= section-tab :questions)
                                                                  " selected"))
                                                :href (-> (get-in cursor [:view :selected-path])
                                                          (assoc :section-tab :questions)
                                                          history-link)}
                                           "Vragen"))))))))
