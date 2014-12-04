(ns studyflow.web.history
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as gevents]
            [clojure.string :as string])
  (:import [goog.history EventType]
           [goog History]))

(def history (History.))

(def text-url-mapping (atom nil))

(defn token->path [token]
  (let [[main-token chapter-text section-text question-token] (string/split token #"/")
        chapter-id (when (seq chapter-text)
                     (get-in @text-url-mapping [:chapter-title->id (keyword chapter-text)]))
        section-id (when (seq section-text)
                     (get-in @text-url-mapping [:chapter-id->section-title->id (keyword chapter-id) (keyword section-text)]))]
    {:main (keyword main-token)
     :chapter-id chapter-id
     :section-id section-id
     :section-tab (if (= question-token "questions")
                    :questions
                    :explanation)}))

(defn path->token [path]
  (let [{:keys [main chapter-id section-id section-tab]} path
        chapter-text (get-in @text-url-mapping [:id->title (keyword chapter-id)])
        section-text (get-in @text-url-mapping [:id->title (keyword section-id)])]
    (string/join "/" [(if main
                        (name main)
                        "dashboard")
                      chapter-text section-text
                      (if (= section-tab :questions)
                        "questions"
                        "text")])))

(defn wrap-history [widgets]
  (fn [cursor owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (gevents/listen history EventType.NAVIGATE
                        (fn [event]
                          (when-let [path (token->path (.-token event))]
                            (om/update! cursor
                                        [:view :selected-path]
                                        path)))))
      om/IRender
      (render [_]
        (om/build widgets cursor))
      om/IDidMount
      (did-mount [_]
        ;; setEnabled fires NAVIGATE event for first load
        (.setEnabled history true)
        ))))

(defn listen [tx-report cursor]
  (let [{:keys [path old-state new-state]} tx-report]
    (when (= path [:view :course-material])
      (reset! text-url-mapping (get-in new-state [:view :course-material :text-url-mapping])))
    ;; TODO: we probably do not need this; navigation should
    ;; always go through window.location, not the other way around
    (when (= path [:view :selected-path])
      (when-let [token (path->token (get-in new-state [:view :selected-path]))]
        (.setToken history token)))))

(defn path-url [selected-path]
  (str "#" (path->token selected-path)))

(defn link-to-path
  [selected-path & content]
  (apply dom/a #js {:href (path-url selected-path)} content))

(defn navigate-to-path
  [selected-path]
  (set! js/window.location.href (path-url selected-path)))
