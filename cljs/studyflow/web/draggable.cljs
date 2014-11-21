(ns studyflow.web.draggable
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [goog.fx.Dragger :as fxdrag]
            [studyflow.web.ipad :refer [ipad?]]))

;; will keep the location between switching calculator on/off
(def position (atom {:top 90
                     :left 90}))

(defn draggable-item
  [view position-cursor]
  (fn [item owner]
    (reify
      om/IRender
      (render [_]
        (dom/div #js {:style
                      #js {:position "fixed"
                           :zIndex 10000
                           :top (if ipad?
                                  10
                                  (:top @position))
                           :left (if ipad?
                                   00
                                   (:left @position))}}
                 (when (and (not ipad?)
                            (om/get-state owner :dragging))
                   (dom/div #js {:style #js {:position "absolute"
                                             :top 40
                                             :width 322
                                             :height 460
                                             :zIndex 9999}}))
                 (om/build view item)))
      om/IDidMount
      (did-mount [_]
        (when-not ipad?
          (let [el (om/get-node owner)
                dragger (goog.fx.Dragger. el)]
            (events/listen dragger
                           fxdrag/EventType.START
                           (fn [event]
                             (om/set-state! owner :dragging true)))
            (events/listen dragger
                           fxdrag/EventType.DRAG
                           (fn [event]
                             (reset! position {:top (. event -top)
                                               :left (. event -left)})))
            (events/listen dragger
                           fxdrag/EventType.END
                           (fn [event]
                             (om/set-state! owner :dragging false)))))))))
