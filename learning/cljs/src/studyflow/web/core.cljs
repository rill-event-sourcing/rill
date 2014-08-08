(ns studyflow.web.core
  (:require [goog.dom :as gdom]
            [goog.string :as gstring]
            [goog.events :as gevents]
            [goog.events.KeyHandler]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.service :as service]
            [studyflow.web.history :as url-history]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defn update-js [js-obj key f]
  (let [key (if (keyword? key)
              (name key)
              key)
        p (.-props js-obj)]
    (aset p key (f (get p key)))
    js-obj))

(defn course-id-for-page []
  (.-value (gdom/getElement "course-id")))

(defn student-id-for-page []
  (.-value (gdom/getElement "student-id")))

(defn student-full-name-for-page []
  (.-value (gdom/getElement "student-full-name")))

(defn logout-target-for-page []
  (.-value (gdom/getElement "logout-target")))

(def app-state (atom {:static {:course-id (course-id-for-page)
                               :student {:id (student-id-for-page)
                                         :full-name (student-full-name-for-page)}
                               :logout-target (logout-target-for-page)}
                      :view {:selected-path {:chapter-id nil
                                             :section-id nil
                                             :dashboard true
                                             :section-tab nil}}
                      :aggregates {}}))

(defn split-text-and-inputs [text inputs]
  (reduce
   (fn [pieces input]
     (loop [[p & ps] pieces
            out []]
       (if-not p
         out
         (if (gstring/contains p input)
           (let [[before & after] (string/split p (re-pattern input))]
             (-> out
                 (into [before input])
                 (into after)
                 (into ps)))
           (recur ps (conj out p))))))
   [text]
   inputs))

(defn history-link [selected-path]
  (str "#" (url-history/path->token selected-path)))

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
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            course (get-in cursor [:view :course-material])
            chapter (some (fn [{:keys [id] :as chapter}]
                            (when (= id chapter-id)
                              chapter)) (:chapters course))]
        (dom/div nil
                 (dom/h1 #js {:data-id (:id course)
                              :className "chapter_title"}
                         (:title chapter))
                 (apply dom/ul nil
                        (for [{:keys [title]
                               section-id :id
                               :as section} (:sections chapter)]
                          (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                (assoc :chapter-id chapter-id
                                                       :section-id section-id)
                                                history-link)
                                      :className (str "section_link "
                                                      (when (= section-id
                                                               (get-in cursor [:view :selected-path :section-id]))
                                                        "selected ")
                                                      (get
                                                       {:finished "finished"
                                                        :in-progress "in_progress"}
                                                       (aggregates/section-test-progress
                                                        (get-in cursor [:aggregates section-id]))
                                                       ""))}
                                 (dom/li #js {:data-id section-id}
                                         title)))))))))

(defn navigation-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            course (get-in cursor [:view :course-material])]
        (dom/nav #js {:id "m-sidenav"}
                 (dom/a #js {:className "dashboard_link"
                             :href  (-> (get-in cursor [:view :selected-path])
                                        (assoc :dashboard true)
                                        history-link)}
                        "Terug naar Dashboard")
                 (if-let [chapter (some (fn [{:keys [id] :as chapter}]
                                          (when (= id chapter-id)
                                            chapter)) (:chapters course))]
                   (om/build navigation cursor)
                   (dom/h2 nil "Menu laden...")))))))

(defn question-by-id [cursor section-id question-id]
  (if-let [question (get-in cursor [:view :section section-id :test question-id])]
    question
    (do (om/update! cursor [:view :section section-id :test question-id] nil)
        nil)))

(defn click-once-button [value onclick & {:keys [enabled]
                                          :or {enabled true}}]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:enabled enabled})
      om/IRender
      (render [_]
        (dom/button #js {:className "button green pull-right"
                         :onClick
                         (fn [_]
                           (onclick)
                           (om/set-state! owner :enabled false))
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
        (println [:render-field field section])
        (dom/span nil
                  (dom/input
                   #js {:react-key (:name field)
                        :ref (:name field)
                        :value (get-in cursor [:view :section section-id :input field-name :given-answer])
                        :onChange (fn [event]
                                    (om/update!
                                     cursor
                                     [:view :section section-id :input field-name :given-answer]
                                     (.. event -target -value)))})

                  (let [input-state (get-in cursor [:view :section section-id :input field-name :input-state])
                        given-answer (get-in cursor [:view :section section-id :input field-name :given-answer])]
                    (println :correct-answers correct-answers :given-answer given-answer :ok? (contains? (set correct-answers) given-answer))
                    (case input-state
                      :answered (if (contains? correct-answers given-answer)
                                   (dom/div nil "Well done")
                                   (dom/div nil "Not well done"))
                      (dom/input
                       #js {:type "submit"
                            :value "change state"
                            :onClick (fn [event]
                                       (om/update!
                                        cursor
                                        [:view :section section-id :input field-name :input-state]
                                        :answered))}))
                    ))))))

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
                                            (if-let [input (get inputs text-or-input)]
                                              input
                                              (dom/span #js {:dangerouslySetInnerHTML #js {:__html text-or-input}} nil))))


                                   ))

                    subsections))))))

(defn section-explanation-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            section (get-in cursor [:view :section section-id :data])]
        (dom/div nil
                 (dom/header #js {:id "m-top_header"}
                             (dom/h1 #js {:className "page_heading"}
                                     (:title section))
                             (dom/a #js {:className "button white small questions"
                                         :href (-> (get-in cursor [:view :selected-path])
                                                   (assoc :section-tab :questions)
                                                   history-link)}
                                    "Vragen"))
                 (if section
                   (om/build section-explanation section)
                   (dom/article #js {:id "m-section"}
                                "Uitleg laden..."))
                 )))))

(defn streak-box [streak owner]
  (reify
    om/IRender
    (render [_]
      (let [streak
            (if (< (count streak) 5)
              (take 5 (concat streak (repeat 5 [nil :open])))
              streak)]
        (apply dom/div #js {:className "streak-box"}
               (map-indexed
                (fn [idx [question-id result]]
                  (dom/span #js {:className (if (<= (- (count streak) 5) idx)
                                              "last-five"
                                              "old")}
                            (condp = result
                              :correct "V"
                              :incorrect "X"
                              :revealed "?"
                              :open "_")))
                streak))))))

(def html->om
  {"a" dom/a, "b" dom/b, "big" dom/big, "br" dom/br, "dd" dom/dd, "div" dom/div,
   "dl" dom/dl, "dt" dom/dt, "em" dom/em, "fieldset" dom/fieldset,
   "h1" dom/h1, "h2" dom/h2, "h3" dom/h3, "h4" dom/h4, "h5" dom/h5, "h6" dom/h6,
   "hr" dom/hr, "i" dom/i, "li" dom/li, "ol" dom/ol, "p" dom/p, "pre" dom/pre,
   "q" dom/q,"s" dom/s,"small" dom/small, "span" dom/span, "strong" dom/strong,
   "sub" dom/sub, "sup" dom/sup, "table" dom/table, "tbody" dom/tbody, "td" dom/td,
   "tfoot" dom/tfoor, "th" dom/th, "thead" dom/thead, "tr" dom/tr, "u" dom/u,
   "ul" dom/ul})

(defn tag-tree-to-om [tag-tree inputs]
  (let [descent (fn descent [tag-tree]
                  (cond
                   (and (map? tag-tree)
                        (contains? tag-tree :tag)
                        (contains? tag-tree :attrs)
                        (contains? tag-tree :content))
                   (let [{:keys [tag attrs content] :as node} tag-tree]
                     (if-let [build-fn (get html->om tag)]
                       (apply build-fn
                              #js {:className (:class attrs)}
                              (map descent content))
                       (cond
                        (= tag "img")
                        (dom/img #js {:src (:src attrs)})
                        (= tag "input")
                        (get inputs (:name attrs))
                        :else
                        (apply dom/span #js {:className "default-html-to-om"}
                               (map descent content)))))
                   (string? tag-tree)
                   tag-tree))]
    (descent tag-tree)))

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
                                                              choice))))))])))
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

(defn modal [content primary-button & [secondary-button]]
  (dom/div #js {:id "m-modal"
                :className "show"}
           (dom/div #js {:className "modal_inner"}
                    content
                    (dom/div #js {:className "modal_footer"}
                             (when secondary-button
                               (update-js secondary-button
                                          :className (fnil (partial str "secundary_action ") "")))
                             (update-js primary-button
                                        :className (partial str "button green primary "))))))

(defn focus-input-box [owner]
  ;; we always call this, even when there's no element called
  ;; "FOCUSED_INPUT". om/get-node can't handle that case
  (when-let [refs (.-refs owner)]
    (when-let [input-ref (aget refs "FOCUSED_INPUT")]
      (when-let [input-field (.getDOMNode input-ref)]
        (when (= "" (.-value input-field))
          (.focus input-field))))))

(defn tool-box
  [tools]
  (apply dom/div #js {:id "toolbox"}
         (map (fn [tool]
                (dom/div #js {:id tool} tool))
              tools)))

(defn single-question-panel [tag-tree inputs]
  (dom/div nil
           (tag-tree-to-om
            (om/value tag-tree)
            inputs)))

(defn reveal-answer-button [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [revealed-answer question-id question-data section-id student-id section-test-aggregate-version course-id]} cursor
            can-reveal-answer (get question-data :has-worked-out-answer)]
        (if can-reveal-answer
          (dom/button #js {:className "button grey pull-right"
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
            _ (om/set-state! owner :submit
                             (fn []
                               (when answering-allowed
                                 (async/put! (om/get-shared owner :command-channel)
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
                               (om/set-state! owner :submit nil)))
            submit (fn []
                     (when-let [f (om/get-state owner :submit)]
                       (f)))
            revealed-answer (get question :worked-out-answer)]
        (dom/div #js {:id "m-section"}
                 (let [progress-modal (get-in cursor [:view :progress-modal])]
                   (condp = progress-modal
                     :show-finish-modal
                     (modal (dom/span nil
                                      (dom/h1 nil "Wohoo!")
                                      (dom/p nil "Je bent klaar met deze paragraaf."))
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
                                   "In deze paragraaf blijven"))
                     :show-streak-completed-modal
                     (modal (dom/h1 nil "Yes!")
                            (dom/p nil "Je hebt deze paragraaf nog een keer voltooid. Nu snap je hem wel :)")
                            (dom/button #js {:onClick (fn [e]
                                                        (submit))}
                                        "Volgende paragraaf"))
                     nil))
                 (om/build streak-box (:streak section-test))
                 (tool-box (:tools question-data))
                 (single-question-panel (:tag-tree question-data)
                                        inputs)
                 (when revealed-answer
                   (dom/div nil
                            "Het uitgewerkte antwoord is: "
                            (dom/div #js {:dangerouslySetInnerHTML #js {:__html revealed-answer}} nil)))
                 (dom/div #js {:id "m-question_bar"}
                          (if answer-correct
                            (if (and finished-last-action
                                     (= progress-modal :launchable))
                              (om/build (click-once-button "Goed! Voltooi paragraaf"
                                                           (fn []
                                                             (submit))) cursor)
                              (om/build (click-once-button
                                         "Goed! Volgende vraag"
                                         (fn []
                                           (submit)
                                           (prn "next question command"))) cursor))
                            (dom/div nil
                                     (om/build (click-once-button "Nakijken"
                                                                  (fn []
                                                                    (submit))
                                                                  :enabled answering-allowed)
                                               cursor)))
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
                                     (when-let [f (om/get-render-state owner :submit)]
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
      (dom/div nil
               (dom/header #js {:id "m-top_header"})
               (dom/article #js {:id "m-section"} "Vragen aan het laden...")
               (dom/div #js {:id "m-question_bar"})))))

(defn section-test [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            section-test (get-in cursor [:aggregates section-id])
            section-title (get-in cursor [:view :section section-id :data :title])]
        (dom/div nil
                 (dom/header #js {:id "m-top_header"}
                             (dom/h1 #js {:className "page_heading"}
                                     section-title)
                             (dom/a #js {:className "button white small questions"
                                         :href (-> (get-in cursor [:view :selected-path])
                                                   (assoc :section-tab :explanation)
                                                   history-link)}
                                    "Uitleg"))
                 (if section-test
                   (let [questions (:questions section-test)
                         question (peek questions)
                         question-id (:question-id question)]
                     (if-let [question-data (question-by-id cursor section-id question-id)]
                       (om/build question-panel cursor)
                       (dom/div nil
                                (dom/header #js {:id "m-top_header"})
                                (dom/article #js {:id "m-section"} "Vraag laden")
                                (dom/div #js {:id "m-question_bar"}))))
                   (om/build section-test-loading cursor)))))))

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
                           (dom/div nil
                                    (dom/header #js {:id "m-top_header"})
                                    (dom/article #js {:id "m-section"}
                                                 "Maak een keuze uit het menu")))
                         (om/build section-test cursor))))))))

(defn sections-navigation [cursor chapter]
  (apply dom/ul nil
         (for [{:keys [title status]
                section-id :id
                :as section} (:sections chapter)]
           (dom/li #js {:data-id section-id}
                   (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                         (assoc :chapter-id (:id chapter)
                                                :section-id section-id
                                                :dashboard false)
                                         history-link)
                               :className (str "section_link "
                                               (when (= section-id
                                                        (get-in cursor [:view :selected-path :section-id]))
                                                 "selected ")
                                               (get {"finished" "finished"
                                                     "in-progress" "in_progress"} status ""))}
                          title)))))


(defn chapter-navigation [cursor selected-chapter-id course chapter]
  (let [selected? (= selected-chapter-id (:id chapter))]
    (dom/li nil
            (dom/h1 #js {:data-id (:id course)
                         :className "chapter_title"}
                    (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                          (assoc :chapter-id (:id chapter)
                                                 :section-id nil
                                                 :dashboard true)
                                          history-link)}
                           (:title chapter)))
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
                   (apply dom/ul nil
                          (map (partial chapter-navigation cursor chapter-id course)
                               (:chapters course))))
          (dom/h2 nil "Hoofdstukken laden..."))))))

(defn dashboard-top-header
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/header #js {:id "m-top_header"}
                  (get-in cursor [:static :student :full-name])
                  (dom/form #js {:method "POST"
                                 :action (get-in cursor [:static :logout-target])
                                 :id "logout-form"}
                            (dom/input #js {:type "hidden"
                                            :name "_method"
                                            :value "DELETE"})
                            (dom/button #js {:type "submit"}
                                        "Uitloggen"))))))



(defn dashboard [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (async/put! (om/get-shared owner :data-channel)
                  ["data/dashboard" (get-in cursor [:static :student :id])]))
    om/IRender
    (render [_]
      (dom/div #js {:id "m-dashboard"}
               "Dashboard"
               (om/build dashboard-top-header cursor)
               (dom/section #js {:id "main"}
                            (om/build dashboard-navigation cursor))))))

(defn widgets [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get-in cursor [:aggregates :failed])
                   (modal
                    (dom/h1 nil "Je bent niet meer up-to-date met de server. Herlaad de pagina.")
                    (dom/button #js {:onClick (fn [e]
                                                (.reload js/location true))}
                                "Herlaad de pagina")))
               (if (get-in cursor [:view :selected-path :dashboard])
                 (om/build dashboard cursor)
                 (dom/div nil
                          (om/build section-panel cursor)
                          (om/build navigation-panel cursor)))))))

(defn ^:export course-page []
  (om/root
   (-> widgets
       service/wrap-service
       url-history/wrap-history)
   app-state
   {:target (gdom/getElement "app")
    :tx-listen (fn [tx-report cursor]
                 (service/listen tx-report cursor)
                 (url-history/listen tx-report cursor))
    :shared {:command-channel (async/chan)
             :data-channel (async/chan)
             :notification-channel (async/chan)}}))
