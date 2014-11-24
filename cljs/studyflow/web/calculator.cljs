(ns studyflow.web.calculator
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.dom :as gdom]
            [goog.events :as events]
            [goog.fx.Dragger :as fxdrag]
            [studyflow.web.ipad :refer [ipad?]]))

(defn focus-calculator []
  (let [iframe (gdom/getElement "calculator-iframe")]
    (when iframe
      (js/setTimeout (fn []
                       (-> iframe
                           .-contentWindow
                           (.focus)))
                     10)
      (js/setTimeout (fn []
                       (-> iframe
                           .-contentWindow
                           (.focus)))
                     100))))

(defn reset-calculator []
  (let [iframe (gdom/getElement "calculator-iframe")]
    (when iframe (-> iframe
                     .-contentWindow
                     (.reset)))))

(defn change-mode-calculator [cursor]
  (let [iframe (gdom/getElement "calculator-iframe")
        calculator-mode (get-in @cursor [:view :calculator-light-mode?])]
    (om/update! cursor [:view :calculator-light-mode?] (not calculator-mode))
    (when iframe (-> iframe
                     .-contentWindow
                     (.chgMode)))))

(defn calculator [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [calculator-light-mode? (get-in cursor [:view :calculator-light-mode?])]
        (dom/div #js {:id "calculator-div"
                      :className (str "calculator-div" (when calculator-light-mode? " light"))
                      :style #js {:display (if (get-in cursor [:view :show-calculator]) "block" "none")}}
                 (dom/div #js {:className "calculator-top-header"
                               :onMouseUp (fn [] (focus-calculator))}
                          (dom/button #js {:className "toggle-light-mode"
                                           :onClick (fn []
                                                      (change-mode-calculator cursor))})
                          (dom/button #js {:className "close-calculator"
                                           :onClick (fn []
                                                      (om/update! cursor [:view :show-calculator] false))}))
                 (dom/iframe #js {:id "calculator-iframe" :name "calculator-iframe" :src "/calculator.html" :width "100%" :height "100%" :frameborder "0" :scrolling "no" :seamless "seamless"}))))))


;; will keep the location between switching calculator on/off
(def position (atom {:top 90
                     :left 90}))

(defn draggable-calculator [item owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style
                    #js {:position "fixed"
                         :zIndex 800 ;; below modals
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
                                           :zIndex 799}}))
               (om/build calculator item)))
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
                           (om/set-state! owner :dragging false))))))))
