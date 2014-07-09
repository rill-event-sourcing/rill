(ns studyflow.web.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.service :as service]
            [studyflow.web.history :as url-history]
            [clojure.string :as string]
            [cljs.core.async :as async]))


(enable-console-print!)

(defn course-id-for-page []
  (let [loc (.. js/document -location -pathname)]
    (last (string/split loc "/"))))

(def app-state (atom {:static {:course-id (course-id-for-page)}
                      :view {:selected-path {:chapter-id nil
                                             :section-id nil
                                             :tab-questions #{}}}
                      :aggregates {}}))

(defn history-link [selected-path]
  (str "#" (url-history/path->token selected-path)))

(defn navigation [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "Navigation will mount"))
    om/IRender
    (render [_]
      (if-let [course (get-in cursor [:view :course-material])]
        (dom/div nil
                 (dom/h1 #js {:data-id (:id course)} (:name course))
                 (apply dom/ul nil
                        (for [{:keys [title sections]
                               chapter-id :id
                               :as chapter} (:chapters course)]
                          (dom/li #js {:data-id chapter-id}
                                  title
                                  (apply dom/ul nil
                                         (for [{:keys [title]
                                                section-id :id
                                                :as section} sections]
                                           (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                                 (assoc :chapter-id chapter-id
                                                                        :section-id section-id)
                                                                 history-link)}
                                                  (dom/li #js {:data-id section-id}
                                                          title
                                                          (when (= section-id
                                                                   (get-in cursor [:view :selected-path :section-id])) "[selected]")))))))))
        (dom/h2 nil "No content ... spinner goes here")))
    om/IWillUnmount
    (will-unmount [_]
      (println "Navigation will unmount"))))

(defn question-by-id [cursor section-id question-id]
  (if-let [question (get-in cursor [:view :section section-id :test question-id])]
    question
    (do (om/update! cursor [:view :section section-id :test question-id] nil)
      nil)))

(defn click-once-button [value onclick]
  (fn [cursor owner]
    (reify
     om/IRender
     (render [_]
       (dom/button #js {:onClick
                        (fn [_]
                          (onclick)
                          (om/set-state! owner :disabled true))}
                   value
                   (when (om/get-state owner :disabled)
                     "[DISABLED]"))))))

(defn section-test [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section-test"}
               (let [section-id (get-in cursor [:view :selected-path :section-id])
                     section-test-id (str "student-idDEFAULT_STUDENT_IDsection-id" section-id)]
                 (if-let [section-test (get-in cursor [:aggregates section-test-id])]
                   (let [questions (:questions section-test)
                         _ (prn "questions: " questions)
                         question (peek questions)
                         question-id (:question-id question)]
                     (if-let [question-data (question-by-id cursor section-id question-id)]
                       (let [text (:text question-data)
                             first-part (.replace text (re-pattern (str "__INPUT_1__.*$")) "")
                             last-part (.replace text (re-pattern (str "^.*__INPUT_1__")) "")
                             current-answer (get-in cursor [:view :section section-id :test :questions question-id :answer])
                             answer-correct (when (contains? question :correct)
                                              (:correct question))]
                         (dom/div nil
                                  (dom/div nil (pr-str (:streak section-test)))
                                  (dom/div nil "Number of questions done: "
                                           (count (filter #{:correct} (:streak section-test))))
                                  (dom/div #js {:dangerouslySetInnerHTML #js {:__html first-part}} nil)
                                  (if answer-correct
                                    (dom/div nil (pr-str (:inputs question)))
                                    (dom/input #js {:value current-answer
                                                    :onChange (fn [event]
                                                                (om/transact!
                                                                 cursor
                                                                 [:view :section section-id :test :questions]
                                                                 #(assoc-in % [question-id :answer] (.. event -target -value))))}))
                                  (when-not (nil? answer-correct)
                                    (dom/div nil (str "Marked as: " answer-correct
                                                      (when answer-correct
                                                        " have some balloons"))))
                                  (dom/div #js {:dangerouslySetInnerHTML #js {:__html last-part}} nil)
                                  (let [course-id (get-in cursor [:static :course-id])]
                                    (if answer-correct
                                      (dom/button #js {:onClick (fn []
                                                                  (async/put! (om/get-shared owner :command-channel)
                                                                              ["section-test-commands/next-question"
                                                                               section-test-id
                                                                               section-id
                                                                               course-id])
                                                                  (prn "next question command"))}
                                                  "Next Question")
                                      (om/build (click-once-button "Check"
                                                                   (fn []
                                                                     (om/update!
                                                                      cursor
                                                                      [:view :section section-id :test :questions question-id :answer]
                                                                      nil)
                                                                     (async/put! (om/get-shared owner :command-channel)
                                                                                 ["section-test-commands/check-answer"
                                                                                  section-test-id
                                                                                  section-id
                                                                                  course-id
                                                                                  question-id
                                                                                  {"__INPUT_1__" current-answer}]))) cursor)))))
                       (dom/div nil "Loading question ...")))
                   (om/build (click-once-button "Start test for this section"
                                                (fn []
                                                  (async/put! (om/get-shared owner :command-channel)
                                                              ["section-test-commands/init" section-id]))) cursor)))))))

(defn section-tabs [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [section-id tab-questions]} (get-in cursor [:view :selected-path])
            tab-selection (if (contains? tab-questions section-id)
                            :questions
                            :explanation)]
        (dom/div nil
                 (apply dom/ul nil
                        (for [[k title] {:explanation "Explanation"
                                         :questions "Questions"}]
                          (if (= tab-selection k)
                            (dom/li nil title)
                            (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                  (update-in [:tab-questions]
                                                             (if (= k :questions) conj disj)
                                                             section-id)
                                                  history-link)}
                                   (dom/li nil title)))))
                 (if (= tab-selection :explanation)
                   (let [section (get-in cursor [:view :section section-id :data])
                         text (get-in section [:subsections-by-level :1-star])]
                     (dom/div nil (pr-str text)))
                   (om/build section-test cursor)))))))

(defn inspect [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (str "Cursor: " (pr-str (-> cursor
                                           (update-in [:view :course-material] (fn [s] (str (subs (pr-str s) 0 5) "...truncated...")))
                                           (update-in [:view :section]
                                                      (fn [s]
                                                        (zipmap (keys s)
                                                                (map (fn [sc] (update-in sc [:data] #(str (subs (pr-str %) 0 5) "...truncated..."))) (vals s))))))))))))


(defn content [cursor owner]
  (reify
    om/IRender
    (render [_]
      (if-let [{:keys [chapter-id section-id]} (get-in cursor [:view :selected-path])]
        (if-let [section-data (get-in cursor [:view :section section-id :data])]
          (dom/div #js {:id (str "section-" section-id)}
                   (dom/h2 nil (:title section-data))
                   (om/build section-tabs cursor)
                   (om/build inspect cursor))
          (dom/h2 nil (str "Selected data not yet loaded" [chapter-id section-id])))
        (dom/h2 nil "No section selected")))))

(defn widgets [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "widget will mount"))
    om/IRender
    (render [_]
      (dom/div nil
               (om/build navigation cursor)
               (om/build content cursor)))
    om/IWillUnmount
    (will-unmount [_]
      (println "widget will unmount"))))

(defn ^:export course-page []
  (om/root
   (-> widgets
       service/wrap-service
       url-history/wrap-history)
   app-state
   {:target (. js/document (getElementById "app"))
    :tx-listen (fn [tx-report cursor]
                 (service/listen tx-report cursor)
                 (url-history/listen tx-report cursor))
    :shared {:command-channel (async/chan)}}))
