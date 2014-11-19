(ns studyflow.web.entry-quiz
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.core :as core]
            [studyflow.web.helpers :refer [input-builders tool-box raw-html modal tag-tree-to-om focus-input-box click-once-button]]
            [studyflow.web.history :refer [path-url navigate-to-path]]
            [studyflow.web.service :as service]
            [studyflow.web.recommended-action :refer [first-recommendable-chapter]]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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
                          (om/build (click-once-button
                                     "Naar de eerste vraag"
                                     (fn []
                                       (submit))) cursor)))))))



(defn to-dashboard-bar [status chapter-id]
  (dom/div #js {:id "m-question_bar"}
           (dom/button #js {:className "btn blue small pull-right"
                            :onClick (fn []
                                       (navigate-to-path {:main :dashboard
                                                          :chapter-id chapter-id}))}
                       (if (or (= status :failed) (= status :passed))
                         "Let's Go!"
                         "Naar je Dashboard"))))


(defn entry-quiz-result [status student-name correct-answers-number total-questions-number link-chapter-id]
  (let [style #js {:width (str (Math/round (float (/ (* 100 correct-answers-number) total-questions-number))) "%;")}]
    (dom/div nil
             (dom/p nil (str "Hoi " student-name))
             (dom/p nil (str "Je had " correct-answers-number " van de " total-questions-number " vragen goed!"))
             (dom/div #js {:className "progress"}
                      (dom/div #js {:className "progress_bar" :style style}
                               (dom/span nil (str correct-answers-number "/" total-questions-number))))
             (if (= status :passed)
               (dom/p nil "We raden je aan om bij hoofdstuk 7 te beginnen.")
               (dom/p nil "We raden je aan om bij het begin te beginnen, zodat je alles nog even kan opfrissen."))
             (to-dashboard-bar status link-chapter-id))))

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
            chapters (:chapters (get-in cursor [:view :course-material]))
            course (get-in cursor [:view :course-material])
            first-non-finished-chapter-id (:id (first-recommendable-chapter course))
            status (:status entry-quiz)
            correct-answers-number (:correct-answers-number entry-quiz)
            student-name (get-in cursor [:static :student :full-name])]
        (dom/div #js {:id "quiz-page"}
                 (dom/header #js {:id "m-top_header"}
                             (dom/a #js {:id "home"
                                         :href (path-url {:main :dashboard})})
                             (dom/h1 #js {:id "page_heading"}
                                     (entry-quiz-title status)) ;; TODO title is not in aggregate
                             (when-let [index (:question-index entry-quiz)]
                               (dom/p #js {:id "quiz_counter"}
                                      (str "Vraag " (inc index) " van " (count (:questions material))))))
                 (dom/section #js {:id "main"}
                              (dom/article #js {:id "m-section"}
                                           (if-not (get-in cursor [:view :entry-quiz-replay-done])
                                             (dom/div nil "Instaptoets laden"
                                                      (to-dashboard-bar status nil))
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
                                                     question-data (get-in material [:questions index])
                                                     question-text (:text question-data)
                                                     current-answers (om/value (get-in cursor [:view :entry-quiz index :answer] {}))
                                                     inputs (input-builders cursor (:id question-data) question-data current-answers true [:view :entry-quiz index :answer])
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
                                                           (tag-tree-to-om (:tag-tree question-data) inputs)
                                                           (dom/div #js {:id "m-question_bar"}
                                                                    (tool-box (:tools question-data))
                                                                    (om/build (click-once-button (str "Klaar"
                                                                                                      (when (< (inc index) (count (:questions material)))
                                                                                                        " & volgende vraag"))
                                                                                                 (fn []
                                                                                                   ;; form handles submit
                                                                                                   nil)
                                                                                                 :enabled answering-allowed)
                                                                              cursor))))
                                               :passed
                                               (dom/div nil (entry-quiz-result :passed student-name correct-answers-number (count (:questions material)) first-non-finished-chapter-id))
                                               :failed
                                               (dom/div nil (entry-quiz-result :failed student-name correct-answers-number (count (:questions material)) (:id (first chapters))))
                                               nil)))))))
    om/IDidMount
    (did-mount [_]
      (focus-input-box owner))))


(defn entry-quiz-modal [cursor owner]
  (when-let [entry-quiz (get-in cursor [:view :course-material :entry-quiz])]
    (let [{:keys [status]
           entry-quiz-id :id} entry-quiz
           status (if (= :dismissed (get-in cursor [:view :entry-quiz-modal]))
                    :dismissed
                    (keyword status))
           course-id (get-in cursor [:static :course-id])
           student-id (get-in cursor [:static :student :id])
           entry-quiz-gif (rand-nth ["https://assets.studyflow.nl/learning/cat-fly.gif"
                                     "https://assets.studyflow.nl/learning/diving-corgi.gif"])
           ;; TODO should come from entry-quiz material

           dismiss-modal (fn []
                           (om/update! cursor [:view :entry-quiz-modal] :dismissed)
                           (async/put! (om/get-shared owner :command-channel)
                                       ["entry-quiz-commands/dismiss-nag-screen"
                                        course-id
                                        student-id]))]
      (condp = status
        nil (modal (dom/div nil
                            (dom/h1 nil "Hoi, welkom op Studyflow!")
                            (dom/img #js {:src entry-quiz-gif})
                            (dom/p nil "Maak een vliegende start en bepaal waar je begint" (dom/br nil) "met de instaptoets:")
                            (dom/ul #js {:className "m-icon_row"}
                                    (dom/li #js {:className "m-icon_row_item time"} "Duurt ongeveer 30 minuten")
                                    (dom/li #js {:className "m-icon_row_item onlyonce"} "Kun je maar " (dom/br nil) "1 keer" (dom/br nil) "doen")
                                    (dom/li #js {:className "m-icon_row_item stopgo"} "Stoppen en later weer verder gaan")))
                   "Instaptoets starten"
                   (fn []
                     (dismiss-modal)
                     (navigate-to-path {:main :entry-quiz}))
                   (dom/a #js {:href ""
                               :onClick (fn []
                                          (dismiss-modal)
                                          false)
                               :className "btn big gray"}
                          "Later maken"))
        :dismissed
        nil ;; show link at the dashboard in a deeper nesting
        :in-progress
        nil ;; show link at the dashboard in a deeper nesting
        nil))))
