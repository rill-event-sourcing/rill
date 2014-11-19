(ns studyflow.web.chapter-quiz
  (:require [cljs.core.async :as async]
            [goog.events :as gevents]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.helpers :refer [input-builders tool-box modal raw-html tag-tree-to-om focus-input-box] :as helpers]
            [studyflow.web.recommended-action :refer [recommended-action]]
            [studyflow.web.history :refer [path-url navigate-to-path]]))

(defn chapter-quiz-navigation-button [cursor chapter-quiz chapter-id]
  (when (not (zero? (:number-of-questions chapter-quiz)))
    (let [chapter-quiz-status (:status chapter-quiz)
          button-class (str "btn chapter-quiz-btn yellow "
                            (case chapter-quiz-status
                              nil "fast-track-btn"
                              "running-fast-track" "fast-track-btn"
                              "passed" "passed"
                              nil))]
      (prn [:status chapter-quiz-status])
      (dom/li #js {:className (str "chapter-quiz " chapter-quiz-status) }
              (dom/button (if (#{"locked" "passed"} chapter-quiz-status)
                            #js {:className button-class
                                 :disabled :disabled}
                            #js {:className button-class
                                 :onClick (fn []
                                            (om/update! cursor [:view :chapter-quiz-modal] {:show true
                                                                                            :chapter-id chapter-id}))})
                          "Hoofdstuktest")))))

(defn chapter-quiz-modal [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :chapter-quiz-modal :chapter-id])
            student-id (get-in cursor [:static :student :id])
            course (get-in cursor [:view :course-material])
            chapter (first (filter (fn [{:keys [id] :as chapter}]
                                     (= id chapter-id)) (:chapters course)))
            chapter-status (:status (:chapter-quiz chapter))
            dismiss-modal (fn [] (om/update! cursor [:view :chapter-quiz-modal :show] false))]
        (modal (if (nil? chapter-status)
                 (dom/div nil
                          (dom/h1 nil "Hoofdstuktest (Snelle route)")
                          (dom/img #js {:src (rand-nth ["https://assets.studyflow.nl/learning/treadmill-cat.gif"
                                                        "https://assets.studyflow.nl/learning/bulldog-waterski.gif"
                                                        "https://assets.studyflow.nl/learning/fast-cat.gif"])})
                          (dom/p nil "Maak de Hoofdstuktest om het hoofdstuk over te slaan." )
                          (dom/p nil "Deze test bevat vragen uit alle paragrafen in dit hoofdstuk.")
                          (dom/ul nil
                                  (dom/li nil "Je kunt de snelle route maar 1 keer proberen")
                                  (dom/li nil "Duurt ongeveer 10-30 minuten")))
                 (dom/div nil
                          (dom/h1 nil "Hoofdstuktest")
                          (dom/img #js {:src (rand-nth ["https://assets.studyflow.nl/learning/glasses-cat.gif"
                                                        "https://assets.studyflow.nl/learning/studyflow-cat.gif"])})
                          (dom/p nil "Maak de Hoofdstuktest om het hoofdstuk af te sluiten." )
                          (dom/p nil "Deze test bevat vragen uit alle paragrafen in dit hoofdstuk.")
                          (dom/ul nil
                                  (dom/li nil "Je kunt de test zo vaak proberen als je wilt")
                                  (dom/li nil "Duurt ongeveer 10-30 minuten")
                                  (dom/li nil "Als je stopt moet je opnieuw"))))
               "Start Hoofdstuktest"
               (fn []
                 (dismiss-modal)
                 (async/put! (om/get-shared owner :command-channel)
                             ["chapter-quiz-commands/start"
                              chapter-id student-id])
                 (navigate-to-path {:main :chapter-quiz
                                    :chapter-id chapter-id}))
               (dom/a #js {:href ""
                           :className "btn big gray"
                           :onClick (fn []
                                      (dismiss-modal)
                                      false)}
                      "Later doen"))))))

(defn footer-bar
  ([]
     (dom/div #js {:id "m-question_bar"}))
  ([text on-click enabled color tools]
     (dom/div #js {:id "m-question_bar"}
              (tool-box tools)
              (dom/button #js {:className (str "btn small pull-right " color)
                               :ref "MAIN_BUTTON"
                               :disabled (not enabled)
                               :onClick (fn []
                                          (helpers/ipad-reset-header)
                                          (on-click))}
                          text))))


(defn chapter-quiz-loading [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            student-id (get-in cursor [:static :student :id])]
        (async/put! (om/get-shared owner :command-channel)
                    ["chapter-quiz-commands/reload"
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
            inputs (input-builders cursor question-id question-data current-answers true
                                   [:view :chapter-quiz chapter-id :test :questions question-id :answer])
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
                              (tag-tree-to-om (:tag-tree question-data) inputs nil nil))
                 (footer-bar "Nakijken" ;; TODO should be "Voltooi
                             ;; test" for final question but that's
                             ;; only the case if we know you can't
                             ;; fail the quiz with this question,
                             ;; using "Nakijken" always at least is consistent
                             (fn []
                               (when-let [f (om/get-state owner :submit)]
                                 (f)))
                             answering-allowed
                             "blue"
                             (:tools question-data)))))
    om/IDidMount
    (did-mount [_]
      (focus-input-box owner)
      (set-key-handler owner))))

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
            current-answers (om/value (get-in cursor [:view :chapter-quiz chapter-id :test :questions question-id :answer] {}))
            inputs (input-builders cursor question-id question-data current-answers false
                                   [:view :chapter-quiz chapter-id :test :questions question-id :answer])
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            last-question (= question-total (count questions))
            failed-quiz (let [wrong-count (get-in cursor [:aggregates chapter-id :questions-wrong-count])]
                          (if (:fast-route? chapter-quiz)
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
                              (tag-tree-to-om (:tag-tree question-data) inputs nil nil))
                 (footer-bar (str "Fout! "
                                  (if failed-quiz
                                    " Stop test"
                                    (if last-question
                                      " Voltooi test"
                                      " Volgende vraag")))
                             (fn []
                               (when-let [f (om/get-state owner :submit)]
                                 (f)))
                             true
                             "red"
                             (:tools question-data)))))
    om/IDidMount
    (did-mount [_]
      (when-let [button (om/get-node owner "MAIN_BUTTON")]
        (.focus button))
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
                              (dom/p nil (str "Hoi " (get-in cursor [:static :student :full-name])))
                              (dom/p nil "Je had " questions-correct-count " van de " question-total " vragen goed!")
                              (dom/p nil "Je hebt hiermee "
                                     (dom/b nil chapter-title)
                                     " afgerond"))
                 (let [{:keys [title link]} (recommended-action cursor)]
                   (footer-bar (str "Ga verder met " title)
                               (fn []
                                 (js/window.location.assign link))
                               true
                               "blue"
                               [])))))))

(defn chapter-quiz-failed [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            questions-correct-count (:questions-correct-count chapter-quiz)
            question-total (get-in cursor [:view :course-material :chapters-by-id chapter-id :chapter-quiz :number-of-questions])
            chapter-title (get-in cursor [:view :course-material :chapters-by-id chapter-id :title])]
        (dom/div nil
                 (dom/article #js {:id "m-section"}
                              (if (:fast-route? chapter-quiz)
                                (dom/div nil
                                         (dom/p nil "Oops! Je hebt 2 hartjes verloren. We raden je aan om eerst je kennis van dit hoofdstuk op te frissen, en het daarna nog een keer te proberen.")
                                         (dom/p nil "Je kunt de test pas weer maken wanneer je alle paragrafen in dit hoofdstuk hebt afgerond."))
                                (dom/div nil
                                         (dom/p nil "Oops! Je hebt 3 hartjes verloren. We raden je aan om eerst je kennis van dit hoofdstuk op te frissen, en het daarna nog een keer te proberen"))))
                 (footer-bar "Ga verder met het hoofdstuk"
                             (fn []
                               (navigate-to-path {:main :dashboard
                                                  :chapter-id chapter-id}))
                             true
                             "blue"
                             []))))))

(defn hearts-bar [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            chapter-quiz (get-in cursor [:aggregates chapter-id])
            questions-wrong-count (get-in cursor [:aggregates chapter-id :questions-wrong-count])
            lives (if (:fast-route? chapter-quiz)
                    2
                    3)
            dead questions-wrong-count
            alive (- lives dead)]
        (apply dom/ul #js {:className "m-heart-bar"}
               (concat (repeatedly dead #(dom/li nil
                                                 (dom/span #js {:className "heart lost-heart"})
                                                 (dom/span #js {:className "heart-gray"})))
                       (repeatedly alive #(dom/li nil
                                                  (dom/span #js {:className "heart"})
                                                  (dom/span #js {:className "heart-gray"})))))))))

(defn chapter-quiz-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [student-id (get-in cursor [:static :student :id])
            chapter-id (get-in cursor [:view :selected-path :chapter-id])
            show-exit-modal (get-in cursor [:view :chapter-quiz-exit-modal])
            dismiss-modal (fn [] (om/update! cursor [:view :chapter-quiz-exit-modal] nil))
            chapter-quiz-agg (get-in cursor [:aggregates chapter-id])
            question-index (inc (:question-index (peek (:questions chapter-quiz-agg))))
            chapter (get-in cursor [:view :course-material :chapters-by-id chapter-id])
            question-total (get-in chapter [:chapter-quiz :number-of-questions])
            chapter-quiz-status (:status chapter-quiz-agg)
            fast-route? (:fast-route? chapter-quiz-agg)]
        (dom/div #js {:id "quiz-page"}
                 (when show-exit-modal
                   (modal (if fast-route?
                            (dom/div nil
                                     (dom/h1 nil "Hoofdstuktest")
                                     (dom/p nil "Weet je zeker dat je de Hoofdstuktest wil stoppen? Als je de test nu stopt, kun je hem pas weer maken wanneer je alle paragrafen in dit hoofdstuk hebt afgerond."))
                            (dom/div nil
                                     (dom/h1 nil "Hoofdstuktest")
                                     (dom/p nil "Weet je zeker dat je de Hoofdstuktest wil stoppen?")
                                     (dom/p nil "Als je de test stopt moet je hem opnieuw maken.")))
                          "Stop Hoofdstuktest"
                          (fn []
                            (dismiss-modal)
                            (async/put! (om/get-shared owner :command-channel)
                                        ["chapter-quiz-commands/stop"
                                         chapter-id student-id]))
                          (dom/a #js {:href ""
                                      :className "btn big gray"
                                      :onClick (fn []
                                                 (dismiss-modal)
                                                 false)}
                                 "Doorgaan")))
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
                                           :href (path-url {:main :dashboard
                                                            :chapter-id chapter-id})}))
                             (dom/h1 #js {:id "page_heading"}
                                     (:title chapter))
                             (when chapter-quiz-agg
                               (om/build hearts-bar cursor))
                             (when (= chapter-quiz-status :running)
                               (dom/div #js {:className "progress" :id "quiz_counter"}
                                        (dom/div #js {:className "progress_bar"
                                                      :style #js {:width
                                                                  (str (Math/round (float (/ (* 100 question-index) question-total))) "%")}}
                                                 (dom/span nil (str "Vraag " question-index  " van " question-total))))))

                 (dom/section #js {:id "main"}
                              (cond
                               (or (nil? chapter-quiz-agg)
                                   (= (:locked chapter-quiz-agg) false))
                               (om/build chapter-quiz-loading cursor)
                               (= chapter-quiz-status :passed)
                               (om/build chapter-quiz-passed cursor)
                               (= chapter-quiz-status :failed)
                               (om/build chapter-quiz-failed cursor)
                               :else
                               (om/build chapter-quiz-question cursor))))))
    om/IWillMount
    (will-mount [_]
      (helpers/ipad-fix-scroll-after-switching))))
