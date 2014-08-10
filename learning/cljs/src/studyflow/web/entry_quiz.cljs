(ns studyflow.web.entry-quiz
  (:require [goog.dom :as gdom]
            [goog.events :as gevents]
            [goog.events.KeyHandler]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.aggregates :as aggregates]
            [studyflow.web.core :as core]
            [studyflow.web.service :as service]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn entry-quiz-id-for-page []
  (.-value (gdom/getElement "entry-quiz-id")))

(defn init-app-state []
  (atom {:static {:entry-quiz-id (entry-quiz-id-for-page)
                  :student {:id (core/student-id-for-page)
                            :full-name (core/student-full-name-for-page)}
                  :logout-target (core/logout-target-for-page)}
         :view {:questions {}}
         :aggregates {}}))

(defn start-panel [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [entry-quiz-id (get-in cursor [:static :entry-quiz-id])
            student-id (get-in cursor [:static :student :id])
            entry-quiz (get-in cursor [:aggregates entry-quiz-id])
            submit (fn []
                    (prn "handle submit")
                    (async/put! (om/get-shared owner :command-channel)
                                ["student-entry-quiz-commands/init"
                                 entry-quiz-id
                                 student-id]))]
        (om/set-state! owner :submit submit)
        (dom/div nil
                 "Start hier"
                 (dom/div #js {:id "m-button_bar"}
                          (om/build (core/click-once-button
                                     "Start instaptoets"
                                     (fn []
                                       (submit))) cursor)))))
    om/IDidMount
    (did-mount [_]
      (let [key-handler (goog.events.KeyHandler. js/document)]
        (when-let [key (om/get-state owner :key-listener)]
          (goog.events/unlistenByKey key))
        (->> (goog.events/listen key-handler
                                 goog.events.KeyHandler.EventType.KEY
                                 (fn [e]
                                   (when (= (.-keyCode e) 13) ;;enter
                                     (when-let [f (om/get-render-state owner :submit)]
                                       (f)))))
             (om/set-state! owner :key-listener))))))

(defn entry-quiz-panel [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (async/put! (om/get-shared owner :data-channel)
                  ["data/entry-quiz"
                   (get-in cursor [:static :entry-quiz-id])
                   (get-in cursor [:static :student :id])]))
    om/IRender
    (render [_]
      (dom/div #js {:id "m-entry-quiz"}
               (dom/header #js {:id "m-top_header"}
                           (dom/div #js {:id "main"}
                                    (if-not (get-in cursor [:view :entry-quiz-replay-done])
                                      (dom/h1 nil "Instaptoets laden")
                                      (let [entry-quiz-id (get-in cursor [:static :entry-quiz-id])
                                            entry-quiz (get-in cursor [:aggregates entry-quiz-id])]
                                        (prn "entry-quiz" entry-quiz)
                                        (condp = (:status entry-quiz)
                                          nil ;; entry-quiz not yet
                                          ;; started
                                          (om/build start-panel cursor)

                                          :in-progress
                                          (dom/div nil "GOed bezig")
                                          :done
                                          (dom/div nil "Je hebt de instaptoets afgerond. Ga terug naar het dashboard")
                                          :abandoned
                                          (dom/div nil "Je hebt de instaptoets niet afgerond. Je kan er niet mee verder. Ga terug naar het dashboard")
                                          (dom/div nil
                                                   "TODO debug:"
                                                   (pr-str entry-quiz)))))))))))

(defn widgets [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get-in cursor [:aggregates :failed])
                 (core/modal
                  (dom/h1 nil "Je bent niet meer up-to-date met de server. Herlaad de pagina.")
                  (dom/button #js {:onClick (fn [e]
                                              (.reload js/location true))}
                              "Herlaad de pagina")))
               (om/build entry-quiz-panel cursor)))))

(defn ^:export entry-quiz-page []
  (om/root
   (-> widgets
       service/wrap-service)
   (init-app-state)
   {:target (gdom/getElement "app")
    :tx-listen (fn [tx-report cursor]
                 (service/listen tx-report cursor))
    :shared {:command-channel (async/chan)
             :data-channel (async/chan)}}))
