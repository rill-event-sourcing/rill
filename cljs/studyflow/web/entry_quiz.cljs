(ns studyflow.web.entry-quiz
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.core :as core]
            [studyflow.web.helpers :refer [raw-html modal tag-tree-to-om]]
            [studyflow.web.history :refer [history-link]]
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
                                                            (raw-html choice)))))))])))
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

(defn instructions-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [course-id (get-in cursor [:static :course-id])
            student-id (get-in cursor [:static :student :id])
            entry-quiz (get-in cursor [:view :course-material :entry-quiz])
            submit (fn []
                     (prn "handle submit")
                     (async/put! (om/get-shared owner :command-channel)
                                 ["entry-quiz-commands/init"
                                  course-id
                                  student-id]))]
        (dom/div nil
                 (raw-html (:instructions entry-quiz))
                 (dom/div #js {:id "m-question_bar"}
                          (om/build (core/click-once-button
                                     "Naar de eerste vraag"
                                     (fn []
                                       (submit))) cursor)))))))


(defn to-dashboard-bar [status]
  (dom/div #js {:id "m-question_bar"}
           (dom/button #js {:className "btn blue small pull-right"
                            :onClick (fn []
                                       (set! (.-location js/window)
                                             (history-link {:main :dashboard})))}
                       (if (or (= status :failed) (= status :passed))
                         "Let's Go!"
                         "Naar je Dashboard"))))

(defn entry-quiz-result [status student-name correct-answer-number total-question-number]
  (let [style #js {:width (str (Math/round (float (/ (* 100 correct-answer-number) total-question-number))) "%;")}]
    (dom/div nil
             (dom/p nil (str "Hoi " student-name))
             (dom/p nil (str "Je had " correct-answer-number " van de " total-question-number " vragen goed!"))
             (dom/div #js {:className "progress"}
                      (dom/div #js {:className "progress_bar" :style style}
                               (dom/span nil (str correct-answer-number "/" total-question-number))))
             (if (= status :passed)
               (dom/p nil "We raden je aan om bij hoofdstuk 7 te beginnen.")
               (dom/p nil "We raden je aan om bij het begin te beginnen, zodat je alles nog even kan opfrissen."))
             (to-dashboard-bar status))))

(defn entry-quiz-title [status]
  (if (or (= status :failed) (= status :passed))
    "Einde toets"
    "Welkom op Studyflow!"))

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
            material (get-in cursor [:view :course-material :entry-quiz])
            status (:status entry-quiz)
            correct (:correct entry-quiz)
            student-name (get-in cursor [:static :student :full-name])]
        (dom/div #js {:id "m-entry-quiz"
                      :className "entry_exam_page"}
                 (dom/header #js {:id "m-top_header"}
                             (dom/a #js {:id "home"
                                         :href (history-link {:main :dashboard})})
                             (dom/h1 #js {:id "page_heading"}
                                     (entry-quiz-title status)) ;; TODO title is not in aggregate
                             (when-let [index (:question-index entry-quiz)]
                               (dom/p #js {:id "quiz_counter"}
                                      (str "Vraag " (inc index) " van " (count (:questions material))))))
                 (dom/section #js {:id "main"}
                              (dom/article #js {:id "m-section"}
                                           (if-not (get-in cursor [:view :entry-quiz-replay-done])
                                             (dom/div nil "Instaptoets laden"
                                                      (to-dashboard-bar status))
                                             (case status
                                               nil ; entry-quiz not yet started
                                               (om/build instructions-panel cursor)
                                               :dismissed
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
                                                                 ["entry-quiz-commands/submit-answer"
                                                                  course-id
                                                                  student-id
                                                                  entry-quiz-aggregate-version
                                                                  current-answers])))]
                                                 (dom/form #js
                                                           {:onSubmit (fn []
                                                                        (submit)
                                                                        false)}
                                                           (tag-tree-to-om (:tag-tree question) inputs)
                                                           (dom/div #js {:id "m-question_bar"}
                                                                    (om/build (core/click-once-button (str "Klaar"
                                                                                                           (when (< (inc index) (count (:questions material)))
                                                                                                             " & volgende vraag"))
                                                                                                      (fn []
                                                                                                        ;; form handles submit
                                                                                                        nil)
                                                                                                      :enabled answering-allowed)
                                                                              cursor))))
                                               :passed
                                               (do
                                                 (println (get-in cursor [:view :entry-quiz]))
                                                 (dom/div nil (entry-quiz-result :passed student-name correct (count (:questions material)))))
                                               :failed
                                               (dom/div nil (entry-quiz-result :failed student-name correct (count (:questions material))))
                                               nil)))))))
    om/IDidMount
    (did-mount [_]
      (core/focus-input-box owner))))


(defn entry-quiz-modal [cursor owner]
  (when-let [entry-quiz (get-in cursor [:view :course-material :entry-quiz])]
    (let [{:keys [status nag-screen-text]
           entry-quiz-id :id} entry-quiz
           status (if (= :dismissed (get-in cursor [:view :entry-quiz-modal]))
                    :dismissed
                    (keyword status))
           course-id (get-in cursor [:static :course-id])
           student-id (get-in cursor [:static :student :id])
           ;; TODO should come from entry-quiz material
           nag-screen-text "<img src=\"https://assets.studyflow.nl/learning/cat-fly.gif\"><p>Maak een vliegende start en bepaal waar je begint<br> met de instaptoets:</p>
                            <ul class=\"m-icon_row\"><li class=\"m-icon_row_item time\">Duurt ongeveer 30 minuten</li><li class=\"m-icon_row_item onlyonce\">Kun je maar <br>1 keer<br> doen</li><li class=\"m-icon_row_item stopgo\">Stoppen en later weer verder gaan</li></ul>"
           dismiss-modal (fn []
                           (om/update! cursor [:view :entry-quiz-modal] :dismissed)
                           (async/put! (om/get-shared owner :command-channel)
                                       ["entry-quiz-commands/dismiss-nag-screen"
                                        course-id
                                        student-id]))]
      (condp = status
        nil (modal (dom/div nil
                            (dom/h1 nil "Hoi, welkom op Studyflow!")
                            (raw-html nag-screen-text))
                   (dom/button #js {:onClick (fn []
                                               (dismiss-modal)
                                               (set! (.-location js/window)
                                                     (history-link {:main :entry-quiz})))}
                               "Instaptoets starten")
                   (dom/a #js {:href ""
                               :onClick (fn []
                                          (dismiss-modal)
                                          false)}
                          "Later maken"))
        :dismissed
        nil ;; show link at the dashboard in a deeper nesting
        :in-progress
        nil ;; show link at the dashboard in a deeper nesting
        nil))))
