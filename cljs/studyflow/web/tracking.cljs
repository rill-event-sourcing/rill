(ns studyflow.web.tracking
  (:require [cljs.core.async :as async]
            [clojure.string :as string]
            [goog.dom :as gdom]
            [goog.dom.forms :as gforms]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn track-navigation [command-channel student-id tracking-location]
  (async/put! command-channel
              ["tracking-commands/navigation" student-id tracking-location]))

(defn listen [tx-report cursor command-channel]
  (let [{:keys [path old-state new-state]} tx-report]
    (when (and (= path [:view :selected-path])
               (not= (get-in old-state path)
                     (get-in new-state path)))
      (let [selected-path (get-in new-state path)
            student-id (get-in new-state [:static :student :id])]
        (condp = (:main selected-path)
          :dashboard
          (track-navigation command-channel student-id {:main :dashboard})
          :entry-quiz
          (track-navigation command-channel student-id {:main :entry-quiz})
          :learning
          (track-navigation command-channel student-id (select-keys selected-path [:main :section-id :section-tab]))
          :chapter-quiz
          (track-navigation command-channel student-id (select-keys selected-path [:main :chapter-id]))
          nil)))))

(defn listen-location [tx-report cursor]
  (let [{:keys [path old-state new-state]} tx-report]
    (when (and (= path [:view :selected-path])
               (not= (get-in old-state path)
                     (get-in new-state path)))
      (let [selected-path (get-in new-state path)]
        (when-let [chapter-id (:chapter-id selected-path)]
          (gforms/setValue (gdom/getElement "chapter-id") chapter-id))
        (when-let [section-id (:section-id selected-path)]
          (gforms/setValue (gdom/getElement "section-id") section-id))))))
