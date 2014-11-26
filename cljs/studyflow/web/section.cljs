(ns studyflow.web.section
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.calculator :as calculator]
            [studyflow.web.history :refer [path-url navigate-to-path]]
            [studyflow.web.helpers :refer [input-builders tool-box modal raw-html tag-tree-to-om focus-input-box section-explanation-url on-enter click-once-button] :as helpers]
            [studyflow.web.ipad :as ipad]
            [cljs.core.async :as async]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.chapter-quiz :as chapter-quiz]))

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
                                     (dom/a #js {:href (section-explanation-url cursor chapter section)
                                                 :className "section_link"}
                                            title))))
                         [(chapter-quiz/chapter-quiz-navigation-button cursor (:chapter-quiz chapter) chapter-id)])))))))

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
              input-options (case (:style field)
                              "small" {:class "small-input" :length 5}
                              "exponent" {:class "exponent-input" :length 3}
                              {:class "big-input"})]
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
  [section]
  (-> {}
      (into (for [li (:line-input-fields section)]
              [(:name li) (om/build section-input-field section {:opts {:field li :section section}})]))))

(defn section-reflection
  [cursor owner {:keys [reflection section]}]
  (let [section-id (:id section)
        reflection-path [:view :section section-id :reflection (:name reflection) :open?]
        reflection-open? (get-in cursor reflection-path)]
    (reify
      om/IRender
      (render [_]
        (dom/div #js {:className "m-reflection"}
                 (dom/div #js {:className "reflection-content"}
                          (raw-html (:content reflection)))
                 (dom/button #js {:className "reflection-btn btn gray"
                                  :onClick (fn [event]
                                             (om/update!
                                              cursor
                                              reflection-path
                                              (not reflection-open?)))}
                             (str (if reflection-open? "Verberg" "Toon") " antwoord"))
                 (dom/div #js {:className (str "reflection-answer"
                                               (when reflection-open? " show"))}
                          (raw-html (:answer reflection))))))))

(defn section-extra-example
  [cursor owner {:keys [extra-example section]}]
  (let [section-id (:id section)
        example-path [:view :section section-id :extra-example (:name extra-example) :open?]
        example-open? (get-in cursor example-path)]
    (reify
      om/IRender
      (render [_]
        (when (nil? example-open?)
          (om/update! cursor example-path (:default-open extra-example)))
        (dom/div #js {:className "m-extra-example"}
                 (dom/div #js {:className "extra-example-title"} (:title extra-example))
                 (dom/button #js {:className "extra-example-btn btn blue"
                                  :onClick (fn [event]
                                             (om/update!
                                              cursor
                                              example-path
                                              (not example-open?)))}
                             (str (if example-open? "Verberg" "Toon") " voorbeeld"))
                 (dom/div #js {:className (str "extra-example-content"
                                               (when example-open? " show"))}
                          (raw-html (:content extra-example))))))))

(defn reflection-builder [section]
  (-> {}
      (into (for [reflection (:reflections section)]
              [(:name reflection)  (om/build section-reflection section {:opts {:reflection reflection :section section}})]))))

(defn extra-example-builder [section]
  (-> {}
      (into (for [extra-example (:extra-examples section)]
              [(:name extra-example)  (om/build section-extra-example section {:opts {:extra-example extra-example :section section}})]))))

(defn section-explanation [section owner]
  (reify
    om/IRender
    (render [_]
      (let [subsections (get section :subsections)
            inputs (input-builders-subsection section)
            reflections (reflection-builder section)
            extra-examples (extra-example-builder section)]
        (println [:inputs! inputs])
        (apply dom/article #js {:id "m-section"}
               #_(dom/nav #js {:id "m-minimap"}
                          (apply dom/ul nil
                                 (for [{:keys [title id]
                                        :as subsection} subsections]
                                   (dom/li nil title))))
               (map (fn [{:keys [title tag-tree id] :as subsection}]
                      (dom/section #js {:className "m-subsection"}
                                   (tag-tree-to-om tag-tree inputs reflections extra-examples)))
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
                             (ipad/ipad-reset-header)
                             (async/put! (om/get-shared owner :command-channel)
                                         ["section-test-commands/reveal-worked-out-answer"
                                          section-id
                                          student-id
                                          section-test-aggregate-version
                                          course-id
                                          question-id]))}
                      "Toon antwoord")
          (dom/span nil nil))))))

(defn finish-modal
  [cursor owner student-id course-id chapter-id section-id section-test-aggregate-version]
  (let [finish-section-gif (rand-nth ["https://assets.studyflow.nl/learning/206.gif"
                                      "https://assets.studyflow.nl/learning/haters.gif"
                                      "https://assets.studyflow.nl/learning/helping-dogs.gif"
                                      "https://assets.studyflow.nl/learning/sewing.gif"
                                      "https://assets.studyflow.nl/learning/milk.gif"])
        next-section-path (-> (get-in cursor [:view :selected-path])
                              om/value
                              (merge (om/value (get-in cursor [:view :course-material :forward-section-links
                                                               {:chapter-id chapter-id :section-id section-id}]))))
        submit (fn []
                 (navigate-to-path next-section-path))]
    (modal (dom/span nil
                     (dom/h1 nil "Yes! Je hebt 5 vragen achter elkaar goed!")
                     (dom/img #js {:src finish-section-gif})
                     (dom/p nil "Deze paragraaf is nu klaar. Ga verder naar de volgende paragraaf (of blijf nog even oefenen)." ))
           "Volgende paragraaf" submit
           (dom/a #js {:href ""
                       :className "btn big gray"
                       :onClick (fn [e]
                                  (async/put! (om/get-shared owner :command-channel)
                                              ["section-test-commands/next-question"
                                               section-id
                                               student-id
                                               section-test-aggregate-version
                                               course-id])
                                  false)}
                  "Blijven oefenen"))))

(defn stuck-modal
  [cursor owner student-id course-id chapter-id section-id section-test-aggregate-version]
  (let [explanation-path (-> cursor
                             (get-in [:view :selected-path])
                             om/value
                             (assoc :section-tab :explanation))
        stumbling-gif "https://assets.studyflow.nl/learning/187.gif"
        submit (fn []
                 (async/put! (om/get-shared owner :command-channel)
                             ["section-test-commands/dismiss-modal"
                              section-id
                              student-id
                              section-test-aggregate-version
                              course-id])
                 (navigate-to-path explanation-path))]
    (modal (dom/span nil
                     (dom/h1 #js {:className "stumbling_block"} "Oeps! deze is moeilijk")
                     (dom/img #js {:src stumbling-gif})
                     (dom/p nil "We raden je aan om de uitleg nog een keer te lezen." (dom/br nil) "Dan worden de vragen makkelijker!"))
           "Uitleg lezen" submit)))

(defn completed-modal
  [cursor owner student-id course-id chapter-id section-id section-test-aggregate-version]
  (let [complete-again-section-gif "https://assets.studyflow.nl/learning/184.gif"
        next-section-path (-> (get-in cursor [:view :selected-path])
                              om/value
                              (merge (om/value (get-in cursor [:view :course-material :forward-section-links
                                                               {:chapter-id chapter-id :section-id section-id}]))))
        submit (fn []
                 (navigate-to-path next-section-path))]
    (modal (dom/span nil
                     (dom/h1 nil "Hoppa! Weer goed!")
                     (dom/img #js {:src complete-again-section-gif})
                     (dom/p nil "Je hebt deze paragraaf nog een keer voltooid." (dom/br nil) "We denken dat je hem nu wel snapt :)."))
           "Volgende paragraaf" submit
           (dom/a #js {:href ""
                       :className "btn big gray"
                       :onClick (fn [e]
                                  (async/put! (om/get-shared owner :command-channel)
                                              ["section-test-commands/next-question"
                                               section-id
                                               student-id
                                               section-test-aggregate-version
                                               course-id])
                                  false)}
                  "Blijven oefenen"))))

(defn question-panel [cursor owner]
  (reify
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
            revealed-answer (get question :worked-out-answer)
            modal (get #{:completed-modal :finished-modal :stuck-modal} (:view section-test))
            _ (when-not (contains? (set (:tools question-data)) "calculator")
                (om/update! cursor [:view :show-calculator] false))
            submit (cond modal
                         (fn [] false)

                         answer-correct
                         (fn []
                           (async/put! (om/get-shared owner :command-channel)
                                       ["section-test-commands/next-question"
                                        section-id
                                        student-id
                                        section-test-aggregate-version
                                        course-id]))

                         :else
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
                               current-answers]))))]

        (dom/div #js {:onKeyPress (on-enter submit)}
                 (case modal
                   :finished-modal (finish-modal cursor owner student-id course-id chapter-id section-id section-test-aggregate-version)
                   :stuck-modal (stuck-modal cursor owner student-id course-id chapter-id section-id section-test-aggregate-version)
                   :completed-modal (completed-modal cursor owner student-id course-id chapter-id section-id section-test-aggregate-version)
                   nil)

                 (dom/article #js {:id "m-section"}
                              (tag-tree-to-om (:tag-tree question-data) inputs nil nil)
                              (when revealed-answer
                                (dom/div #js {:className "wrap-dangerous-html"}
                                         (dom/div #js {:dangerouslySetInnerHTML #js {:__html revealed-answer}} nil))))
                 (dom/div #js {:id "m-question_bar"}
                          (tool-box cursor (:tools question-data))
                          (if answer-correct
                            ;; this doesn't have the disabled handling
                            ;; as all the click-once-buttons because
                            ;; we need the ref for set-focus
                            (dom/button (if modal
                                          #js {:className "btn green pull-right"
                                               :disabled :disabled}
                                          #js {:className "btn green pull-right"
                                               :ref "FOCUSED_BUTTON"
                                               :onClick submit})
                                        "Goed! Ga verder")
                            (om/build (click-once-button "Nakijken"
                                                         submit
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
      (ipad/ipad-fix-scroll-after-switching))))

(defn section-test-loading [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (ipad/ipad-fix-scroll-after-switching)
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
        (dom/div nil
                 (om/build calculator/draggable-calculator cursor)
                 (if-let [section-test (get-in cursor [:aggregates section-id])]
                   (let [questions (:questions section-test)
                         question (peek questions)
                         question-id (:question-id question)]
                     (if-let [question-data (question-by-id cursor section-id question-id)]
                       (om/build question-panel cursor)
                       (dom/article #js {:id "m-section"} "Vraag laden")))
                   (om/build section-test-loading cursor)))))))

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
                                           path-url)})
                    (dom/ul #js {:className "section-toggle"}
                            (dom/li nil
                                    (dom/a #js {:className (str "section-toggle-link explanation-link"
                                                                (when (= section-tab :explanation)
                                                                  " selected"))
                                                :href (-> (get-in cursor [:view :selected-path])
                                                          (assoc :section-tab :explanation)
                                                          path-url)}
                                           "Uitleg"))
                            (dom/li nil
                                    (dom/a #js {:className (str "section-toggle-link questions-link"
                                                                (when (= section-tab :questions)
                                                                  " selected"))
                                                :href (-> (get-in cursor [:view :selected-path])
                                                          (assoc :section-tab :questions)
                                                          path-url)}
                                           "Vragen"))))))))
