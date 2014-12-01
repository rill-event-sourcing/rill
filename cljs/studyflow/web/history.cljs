(ns studyflow.web.history
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as gevents]
            [clojure.string :as string])
  (:import [goog.history EventType]
           [goog History]))

(def history (History.))

(defn token->path [token]
  (let [[main-token chapter-id section-id question-token subsection-index] (string/split token #"/")]
    (let [parsed-subsection (.parseInt js/window subsection-index)]
      {:main (keyword main-token)
       :chapter-id (when (seq chapter-id)
                     chapter-id)
       :section-id (when (seq section-id)
                     section-id)
       :section-tab (if (= question-token "questions")
                      :questions
                      :explanation)
       :subsection-index (if (js/isNaN parsed-subsection)
                           0
                           parsed-subsection)})))

(defn path->token [path]
  (let [{:keys [main chapter-id section-id section-tab subsection-index]} path]
    (string/join "/" [(if main
                        (name main)
                        "dashboard")
                      chapter-id section-id
                      (if (= section-tab :questions)
                        "questions"
                        "text")
                      subsection-index])))

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

;; TODO: we probably do not need this; navigation should
;; always go through window.location, not the other way around
(defn listen [tx-report cursor]
  (let [{:keys [path old-state new-state]} tx-report]
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
