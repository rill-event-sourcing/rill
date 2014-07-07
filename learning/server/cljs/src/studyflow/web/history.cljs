(ns studyflow.web.history
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as gevents]
            [clojure.string :as string])
  (:import [goog.history Html5History EventType]))

(def history (Html5History.))

(defn token->path [token]
  (let [chapter-id (-> token
                       (.replace #"section-.*$" "")
                       (.replace "chapter-" ""))
        section-id (-> token
                       (.replace #"tab-.*$" "")
                       (.replace #".*section-" ""))
        tab-ids (-> token
                   (.replace  #".*tab-" "")
                   (string/split "|")
                   set)]
    (when (and (seq chapter-id) (seq section-id))
      {:chapter-id chapter-id
       :section-id section-id
       :tab-questions tab-ids})))

(defn path->token [path]
  (let [{:keys [chapter-id section-id tab-questions]} path]
    (when (and chapter-id section-id)
      (let [tab-questions (string/join "|" tab-questions)]
        (str "chapter-" chapter-id "section-" section-id "tab-" tab-questions)))))

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
    (when (= path [:view :selected-path])
      (when-let [token (path->token (get-in new-state [:view :selected-path]))]
        (.setToken history token)))))

