(ns studyflow.web.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.service :as service]
            [studyflow.web.history :as url-history]
            [clojure.string :as string]
            [cljs.core.async :as async]))


(enable-console-print!)

(defn course-id-for-page []
  (let [loc (.. js/document -location -pathname)]
    (last (string/split loc "/"))))

(defn student-id-for-page []
  (.-value (gdom/getElement "student-id")))

(def app-state (atom {:static {:course-id (course-id-for-page)
                               :student-id (student-id-for-page)}
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
      (let [chapter-id (get-in cursor [:view :selected-path :chapter-id])
            course (get-in cursor [:view :course-material])]
        (if-let [chapter (some (fn [{:keys [id] :as chapter}]
                                 (when (= id chapter-id)
                                   chapter)) (:chapters course))]
          (dom/div nil
                   (dom/div #js {:className "panel panel-default"}
                            (dom/a #js {:href ""}
                                   "< Dashboard")
                            (dom/h1 nil "Course: "(:name course))
                            (dom/h1 #js {:data-id (:id course)}
                                    "Chapter: "
                                    (:title chapter)))
                   (dom/div #js {:className "panel panel-default"}
                            (apply dom/ul nil
                                   (for [{:keys [title]
                                          section-id :id
                                          :as section} (:sections chapter)]
                                     (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                           (assoc :chapter-id chapter-id
                                                                  :section-id section-id)
                                                           history-link)}
                                            (dom/li #js {:data-id section-id}
                                                    title
                                                    (when (= section-id
                                                             (get-in cursor [:view :selected-path :section-id])) "[selected]")))))))
          (dom/h2 nil "No content ... spinner goes here"))))
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

(defn section-explanation [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id tab-questions]} (get-in cursor [:view :selected-path])]
        (apply dom/div #js {:className "row"}
               (apply dom/div #js {:className "col-md-12 panel panel-default"}
                      (when (get-in cursor [:view :selected-path :section-id])
                        [(dom/div #js {:className "col-md-8"} "?????? title is what ????")
                         (dom/div #js {:className "col-md-4"}
                                  (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                        (update-in [:tab-questions]
                                                                   conj section-id)
                                                        history-link)}
                                         "=> Vragen"))]))
               (if-let [section (get-in cursor [:view :section section-id :data])]
                 (let [subsections (get section :subsections)]
                   [(apply dom/div #js {:className "col-md-8 panel panel-default"}
                           (mapcat (fn [{:keys [title text id] :as subsection}]
                                     [(dom/h3 nil title)
                                      (dom/div nil
                                               (pr-str (repeat 100 text)))])
                                   subsections))
                    (dom/div #js {:className "col-md-4 panel panel-default"}
                             (apply dom/ul nil
                                    (for [{:keys [title id]
                                           :as subsection} subsections]
                                      (dom/li nil title))))])
                 ["Loading section data..."])
)))))

(defn streak-box [streak owner]
  (reify
    om/IRender
    (render [_]
      (let [streak
            (if (< (count streak) 4)
              (take 4 (concat streak (repeat 4 [nil :open])))
              streak)]
        (apply dom/div #js {:className "streak-box"}
               (map-indexed
                (fn [idx [question-id result]]
                  (dom/span #js {:className (if (<= (- (count streak) 4) idx)
                                              "last-four"
                                              "old")}
                            (condp = result
                              :correct "V"
                              :incorrect "X"
                              :open "_")))
                streak))))))

(defn focused-input [name js-props]
  (fn [cursor owner]
    (reify
      om/IRender
      (render [_]
        (dom/input js-props))
      om/IDidMount
      (did-mount [_]
        (when-let [input-field (om/get-node owner name)]
          (.focus input-field))))))

(defn question-panel [cursor owner {:keys [section-test
                                           section-id
                                           student-id
                                           question
                                           question-data
                                           chapter-id question-id] :as opts}]
  (reify
    om/IRender
    (render [_]
      (let [text (:text question-data)
            first-part (.replace text (re-pattern (str "_INPUT_1_.*$")) "")
            last-part (.replace text (re-pattern (str "^.*_INPUT_1_")) "")
            question-index (:question-index question)
            current-answers (->> (get-in cursor [:view :section section-id :test :questions [question-id question-index] :answer] {})
                                ;; deref permanently
                                (into {}))
            answer-correct (when (contains? question :correct)
                             (:correct question))
            course-id (get-in cursor [:static :course-id])
            section-test-aggregate-version (:aggregate-version section-test)
            check-answer (fn []
                           (async/put! (om/get-shared owner :command-channel)
                                       ["section-test-commands/check-answer"
                                        section-id
                                        student-id
                                        section-test-aggregate-version
                                        course-id
                                        question-id
                                        current-answers]))
            inputs (-> {}
                       (into (for [mc (:multiple-choice-input-fields question-data)]
                               (let [input-name (:name mc)]
                                 [input-name
                                  (apply dom/ul nil
                                         (for [choice (map :value (:choices mc))]
                                           (dom/li nil
                                                   (dom/input #js {:id choice
                                                                   :type "radio"
                                                                   :checked (= choice (get current-answers input-name))
                                                                   :onChange (fn [event]
                                                                               (om/update!
                                                                                cursor
                                                                                [:view :section section-id :test :questions [question-id question-index] :answer input-name]
                                                                                choice))}
                                                              (dom/label #js {:htmlFor choice} choice)))))])))
                       (into (for [[li dom-fn] (map list
                                                    (:line-input-fields question-data)
                                                    (cons (fn [ref props]
                                                            (om/build (focused-input ref props) cursor))
                                                          (repeat (fn [ref props]
                                                                    (dom/input props)))))]
                               (let [input-name (:name li)]
                                 [input-name
                                 (dom-fn
                                  input-name
                                  #js {:value (get current-answers input-name)
                                       :ref input-name
                                       :onChange (fn [event]
                                                   (om/update!
                                                    cursor
                                                    [:view :section section-id :test :questions [question-id question-index] :answer input-name]
                                                    (.. event -target -value)))
                                       :onKeyPress (fn [event]
                                                     (when (= (.-keyCode event) 13) ;; enter
                                                       (check-answer)))})]))))
            answering-allowed (every? (fn [input-name]
                                        (seq (get current-answers input-name)))
                                      (keys inputs))]
        (dom/div #js {:className "col-md-12 panel panel-default"}
                 (dom/div nil (pr-str question-data))
                 (om/build streak-box (:streak section-test))
                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html first-part}} nil)
                 (if answer-correct
                   (dom/div nil (pr-str (:inputs question)))
                   (apply dom/div nil
                          (-> ["List of inputs here"]
                              (into (vals inputs))
                              (into ["some text here"]))))
                 (when-not (nil? answer-correct)
                   (dom/div nil (str "Marked as: " answer-correct
                                     (when answer-correct
                                       " have some balloons"))))
                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html last-part}} nil)
                 (if answer-correct
                   (if-not (:finished section-test)
                     (dom/button #js {:onClick (fn []
                                                 (async/put! (om/get-shared owner :command-channel)
                                                             ["section-test-commands/next-question"
                                                              section-id
                                                              student-id
                                                              section-test-aggregate-version
                                                              course-id])
                                                 (prn "next question command"))}
                                 "Correct! Next Question")
                     (dom/button #js {:onClick (fn []
                                                 (js/alert "Well done, continue or go to next section"))}
                                 "Correct! Finished Section"))
                   (om/build (click-once-button (if answering-allowed
                                                  "Check"
                                                  "Check [DISABLED]")
                                                (fn []
                                                  (check-answer))) cursor)))))))

(defn section-test [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [section-id (get-in cursor [:view :selected-path :section-id])
            student-id (get-in cursor [:static :student-id])
            section-test (get-in cursor [:aggregates section-id])]
        (dom/div #js {:className "row"}
                 (dom/div #js {:className "col-md-12 panel panel-default"}
                          (dom/div #js {:className "col-md-4"}
                                   (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                         (update-in [:tab-questions]
                                                                    disj section-id)
                                                         history-link)}
                                          "<= Explanation"))
                          (dom/div #js {:className "col-md-4"}
                                   "???? Some title ????")
                          (when section-test
                            (dom/div #js {:className "col-md-4"}
                                     (om/build streak-box (:streak section-test)))))
                 (if section-test
                   (let [questions (:questions section-test)
                         question (peek questions)
                         question-id (:question-id question)]
                     (if-let [question-data (question-by-id cursor section-id question-id)]
                       (om/build question-panel cursor {:opts {:section-test section-test
                                                               :question question
                                                               :question-data question-data
                                                               :question-id question-id
                                                               :section-id section-id
                                                               :student-id student-id}})
                       (dom/div nil "Loading question ...")))
                   (dom/div nil "Starting test for this section")))))))

(defn section-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [chapter-id section-id tab-questions]} (get-in cursor [:view :selected-path])
            tab-selection (if (contains? tab-questions section-id)
                            :questions
                            :explanation)]
        (if (= tab-selection :explanation)
          (if section-id
            (om/build section-explanation cursor)
            (dom/div nil "Select a section"))
          (om/build section-test cursor))))))

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


(defn dashboard [cursor owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div nil
             "Dashboard"
             (if-let [course (get-in cursor [:view :course-material])]
               [(dom/h1 nil (:name course))
                (apply dom/ul nil
                        (for [{:keys [title]
                               chapter-id :id
                               :as chapter} (:chapters course)]
                          (dom/li #js {:data-id chapter-id}
                                  (dom/a #js {:href (-> (get-in cursor [:view :selected-path])
                                                        (assoc :chapter-id chapter-id)
                                                        history-link)}
                                         title
                                         (when (= chapter-id
                                                  (get-in cursor [:view :selected-path :chapter-id])) "[selected]")))))]
               [(dom/h2 nil "No content ... spinner goes here")])))))

(defn widgets [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (println "widget will mount"))
    om/IRender
    (render [_]
      (dom/div nil
               (when (get-in cursor [:aggregates :failed])
                 (dom/div #js {:className "reload-alert"}
                          (dom/h1 nil "You are out of sync with the server. Please reload the page")
                          (dom/button #js {:onClick (fn [e]
                                                      (.reload js/location true))}
                                      "Reload page")))
               (if-not (get-in cursor [:view :selected-path :chapter-id])
                 (om/build dashboard cursor)
                 (dom/div #js {:className "row"}
                          (dom/div #js {:className "col-md-4"}
                                   (om/build navigation cursor))
                          (dom/div #js {:className "col-md-8"}
                           (om/build section-panel cursor))))))
    om/IWillUnmount
    (will-unmount [_]
      (println "widget will unmount"))))

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
    :shared {:command-channel (async/chan)}}))
