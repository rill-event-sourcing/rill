(ns studyflow.web.chapter-quiz
  (:require [cljs.core.async :as async]
            [goog.events :as gevents]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.helpers :refer [modal raw-html tag-tree-to-om focus-input-box]]
            [studyflow.web.recommended-action :refer [recommended-action]]
            [studyflow.web.history :refer [history-link]]))

(defn chapter-quiz-navigation-button [cursor chapter-quiz-status chapter-id]
  (let [chapter-test-agg (get-in cursor [:aggregates chapter-id])
        button-icon (condp = chapter-quiz-status
                      nil ">>"
                      "locked" "🔒"
                      "unlocked" ""
                      "passed" "✓"
                      "TODO")]
    (dom/li #js {:className (str "chapter-quiz " chapter-quiz-status) }
            (dom/button #js {:className "btn yellow"
                             :onClick (fn []
                                        (om/update! cursor [:view :chapter-quiz-modal] {:show true
                                                                                        :chapter-id chapter-id}))}
                        (str "Chapter quiz " button-icon)))))

(defn chapter-quiz-modal [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :chapter-quiz-modal :chapter-id])
            dismiss-modal (fn [] (om/update! cursor [:view :chapter-quiz-modal :show] false))]
        (modal (dom/h1 nil "Hoi, chapter-quiz modal")
               (dom/button #js {:onClick (fn []
                                           (dismiss-modal)
                                           (set! (.-location js/window)
                                                 (history-link {:main :chapter-quiz
                                                                :chapter-id chapter-id})))}
                           "Start Chapter Quiz")
               (dom/a #js {:href ""
                           :onClick (fn []
                                      (dismiss-modal)
                                      false)}
                      "Later doen"))))))

(defn footer-bar
  ([]
     (dom/div #js {:id "m-question_bar"}))
  ([text on-click enabled]
     (dom/div #js {:id "m-question_bar"}
            (dom/button #js {:className "btn blue small pull-right"
                             :disabled (not enabled)
                             :onClick on-click}
                        text))))

(defn chapter-quiz-loading [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :command-channel)
                    ["chapter-quiz-commands/init-when-nil"
                     chapter-id
                     student-id])))
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])]
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              "Laden")
                 (footer-bar))))))

(defn chapter-quiz-question-by-id [cursor data-channel chapter-id question-id]
  (if-let [question (get-in cursor [:view :chapter-quiz chapter-id :questions question-id])]
    question
    (do (async/put! data-channel
                    ["data/chapter-quiz-question" chapter-id question-id])
        nil)))

(defn set-key-handler [owner]
  (let [key-handler (goog.events.KeyHandler. js/document)]
    (when-let [key (om/get-state owner :key-listener)]
      (goog.events/unlistenByKey key))
    (->> (goog.events/listen key-handler
                             goog.events.KeyHandler.EventType.KEY
                             (fn [e]
                               (when (= (.-keyCode e) 13) ;;enter
                                 (when-let [f (om/get-state owner :submit)]
                                   (f)))))
         (om/set-state-nr! owner :key-listener))))

(defn input-builders-enabled
  "mapping from input-name to create react dom element for input type"
  [cursor chapter-id question-id question-data current-answers]
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
                                                      :react-key (str question-id "-" input-name "-" choice)
                                                      :type "radio"
                                                      :checked (= choice (get current-answers input-name))
                                                      :onChange (fn [event]
                                                                  (om/update!
                                                                   cursor
                                                                   [:view :chapter-quiz chapter-id :test :questions question-id :answer input-name]
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
                                 :react-key (str question-id "-" ref)
                                 :ref ref
                                 :onChange (fn [event]
                                             (om/update!
                                              cursor
                                              [:view :chapter-quiz chapter-id :test :questions question-id :answer input-name]
                                              (.. event -target -value)))})
                           (when-let [suffix (:suffix li)]
                             (str " " suffix)))])))))

(defn chapter-quiz-question-open [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            questions (:questions chapter-quiz)
            question (peek questions)
            question-id (:question-id question)
            data-channel (om/get-shared owner :data-channel)
            question-data (chapter-quiz-question-by-id cursor data-channel chapter-id question-id)
            course-id (get-in cursor [:static :course-id])
            chapter-quiz-aggregate-version (:aggregate-version chapter-quiz)
            current-answers (om/value (get-in cursor [:view :chapter-quiz chapter-id :test :questions question-id :answer] {}))
            inputs (input-builders-enabled cursor chapter-id question-id question-data current-answers)
            answering-allowed (every? (fn [input-name]
                                        (seq (get current-answers input-name)))
                                      (keys inputs))]
        (om/set-state-nr! owner :submit
                          (fn []
                            (when answering-allowed
                              (async/put!
                               (om/get-shared owner :command-channel)
                               ["chapter-quiz-commands/check-answer"
                                chapter-id
                                student-id
                                chapter-quiz-aggregate-version
                                course-id
                                question-id
                                current-answers])
                              (om/set-state-nr! owner :submit nil))))
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              (tag-tree-to-om (:tag-tree question-data) inputs))
                 (footer-bar "Nakijken" ;; TODO should be "Voltooi
                             ;; toets" for final question but that's
                             ;; only the case if we know you can't
                             ;; fail the quiz with this question,
                             ;; using "Nakijken" always at least is consistent
                             (fn []
                               (when-let [f (om/get-state owner :submit)]
                                 (f)))
                             answering-allowed))))
    om/IDidMount
    (did-mount [_]
      (focus-input-box owner)
      (set-key-handler owner))))

(defn input-builders-disabled
  "mapping from input-name to create react dom element for input type"
  [cursor chapter-id question-id question-data answers]
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
                                                      :react-key (str question-id "-" input-name "-" choice)
                                                      :type "radio"
                                                      :checked (= choice (get answers (keyword input-name)))
                                                      :disabled true}
                                                 (dom/label #js {:htmlFor id}
                                                            (raw-html choice)))))))])))
      (into (for [[li ref] (map list
                                (:line-input-fields question-data)
                                (map :name (:line-input-fields question-data)))]
              (let [input-name (:name li)]
                [input-name
                 (dom/span nil
                           (when-let [prefix (:prefix li)]
                             (str prefix " "))
                           (dom/input
                            #js {:value (get answers (keyword input-name) "")
                                 :react-key (str question-id "-" ref)
                                 :disabled true})
                           (when-let [suffix (:suffix li)]
                             (str " " suffix)))])))))

(defn chapter-quiz-question-wrong [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            chapter-quiz-status (:status chapter-quiz)
            questions (:questions chapter-quiz)
            question (peek questions)
            question-id (:question-id question)
            data-channel (om/get-shared owner :data-channel)
            question-data (chapter-quiz-question-by-id cursor data-channel chapter-id question-id)
            course-id (get-in cursor [:static :course-id])
            chapter-quiz-aggregate-version (:aggregate-version chapter-quiz)
            answers (om/value (:inputs question))
            inputs (input-builders-disabled cursor chapter-id question-id question-data answers)
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            last-question (= question-total (count questions))
            failed-quiz (let [wrong-count (get-in cursor [:aggregates chapter-id :questions-wrong-count])]
                          (if (:fast-route chapter-quiz)
                            (= 2 wrong-count)
                            (= 3 wrong-count)))]
        (om/set-state-nr! owner :submit
                          (fn []
                            (async/put!
                             (om/get-shared owner :command-channel)
                             ;; TODO
                             ["chapter-quiz-commands/next-question"
                              chapter-id
                              student-id
                              chapter-quiz-aggregate-version
                              course-id])
                            (om/set-state-nr! owner :submit nil)))
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              (tag-tree-to-om (:tag-tree question-data) inputs))
                 (footer-bar (str "Fout! "
                                  (if failed-quiz
                                    " Stop toets"
                                    (if last-question
                                      " Voltooi toets"
                                      " Volgende vraag")))
                             (fn []
                               (when-let [f (om/get-state owner :submit)]
                                 (f)))
                             true))))
    om/IDidMount
    (did-mount [_]
      (set-key-handler owner))))

(defn chapter-quiz-question [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id]} (get-in cursor [:view :selected-path])
            student-id (get-in cursor [:static :student :id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            questions (:questions chapter-quiz)
            question (peek questions)
            question-id (:question-id question)
            data-channel (om/get-shared owner :data-channel)
            question-data (chapter-quiz-question-by-id cursor data-channel chapter-id question-id)
            answer-correct (when (contains? question :correct)
                             (:correct question))]
        (condp = answer-correct
          nil
          (if question-data
            (om/build chapter-quiz-question-open cursor)
            (dom/div nil "Vraag laden"))
          true ;; QuestionAssigned will render the next one, this case
          ;; can be skipped?
          (dom/div nil "Vraag laden")
          false
          (if question-data
            (om/build chapter-quiz-question-wrong cursor)
            (dom/div nil "Vraag laden")))))))

(defn chapter-quiz-passed [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            questions-correct-count (get-in cursor [:aggregates chapter-id :questions-correct-count])
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            chapter-title (get-in cursor [:view :course-material :chapters-by-id chapter-id :title])]
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              (dom/p nil "Je had " questions-correct-count " van de " question-total " vragen goed")
                              (dom/p nil "Je hebt hiermee "
                                     (dom/b nil chapter-title)
                                     " afgerond"))
                 (let [{:keys [title link]} (recommended-action cursor)]
                   (footer-bar (str "Ga verder met " title)
                               (fn []
                                 (js/window.location.assign link))
                               true)))))))

(defn chapter-quiz-failed [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            questions-correct-count (get-in cursor [:aggregates chapter-id :questions-correct-count])
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            chapter-title (get-in cursor [:view :course-material :chapters-by-id chapter-id :title])]
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              (dom/p nil "FAILED")
                              (dom/p nil "Je had " questions-correct-count " van de " question-total " vragen goed"))
                 (footer-bar "Ga verder met het hoofdstuk"
                             (fn []
                               (js/window.location.assign
                                (history-link {:main :dashboard
                                               :chapter-id chapter-id})))
                             true))))))

(defn hearts-bar [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            questions-wrong-count (get-in cursor [:aggregates chapter-id :questions-wrong-count])
            lives (if (:fast-route chapter-quiz)
                    2
                    3)
            dead questions-wrong-count
            alive (- lives dead)]
        (apply dom/div nil
               "Hearts: "
               (concat (repeatedly dead #(dom/span nil "X"))
                       (repeatedly alive #(dom/span nil "O"))))))))

(defn chapter-quiz-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            show-exit-modal (get-in cursor [:view :chapter-quiz-exit-modal])
            dismiss-modal (fn [] (om/update! cursor [:view :chapter-quiz-exit-modal] nil))
            chapter-quiz-agg (get-in cursor [:aggregates chapter-id])
            question-index (inc (:question-index (peek (:questions chapter-quiz-agg))))
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            chapter-quiz-status (:status chapter-quiz-agg)]
        (dom/div #js {:id "m-chapter-quiz"
                      :className "chapter_quiz_page"}
                 (when show-exit-modal
                   (modal (dom/h1 nil "Leaving means stopping the chapter-quiz")
                          (dom/button #js {:onClick (fn []
                                                      (dismiss-modal)
                                                      (set! (.-location js/window)
                                                            (history-link {:main :dashboard
                                                                           :chapter-id chapter-id})))}
                                      "Stop Chapter Quiz")
                          (dom/a #js {:href ""
                                      :onClick (fn []
                                                 (dismiss-modal)
                                                 false)}
                                 "Continue")))
                 (dom/header #js {:id "m-top_header"}
                             (if (= :running chapter-quiz-status)
                               ;; only need to confirm leaving when
                               ;; answering questions
                               (dom/a #js {:id "home"
                                           :href ""
                                           :onClick (fn [e]
                                                      (om/update! cursor [:view :chapter-quiz-exit-modal] true)
                                                      (.preventDefault e))})
                               (dom/a #js {:id "home"
                                           :href (history-link {:main :dashboard
                                                                :chapter-id chapter-id})}))
                             (dom/h1 #js {:id "page_heading"}
                                     (condp = chapter-quiz-status
                                       :passed "Einde toets"
                                       "Chapter quiz"))
                             (when chapter-quiz-agg
                               (om/build hearts-bar cursor))
                             (when (= chapter-quiz-status :running)
                               (dom/p #js {:id "quiz_counter"}
                                      (str "Vraag " question-index  " van " question-total))))

                 (dom/section #js {:id "main"}
                              (cond
                               (nil? chapter-quiz-agg)
                               (om/build chapter-quiz-loading cursor)
                               (= chapter-quiz-status :passed)
                               (om/build chapter-quiz-passed cursor)
                               (= chapter-quiz-status :failed)
                               (om/build chapter-quiz-failed cursor)
                               :else
                               (om/build chapter-quiz-question cursor))))))))
