(ns studyflow.web.draggable
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(def position-chan (chan (sliding-buffer 1)))
(def mouse-mult (mult position-chan))

(defn touch-move
  [e]
  (.preventDefault e)
  (let [touch (aget (.-changedTouches e) 0)]
    {:left (.-clientX touch) :top (.-clientY touch)}))

(.addEventListener js/window "mousemove" #(put! position-chan {:left (.-clientX %) :top (.-clientY %)}))
(.addEventListener js/window "touchmove" #(put! position-chan (touch-move %)))

(defn target-position
  [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    {:top (.-top rect) :left (.-left rect)}))

(defn move-start
  [event-position cursor owner current-position]
  (when (not (om/get-state owner :disabled))
    (let [user-movement (om/get-state owner :user-movement)
          offset {:top (- (.-clientY event-position) (current-position :top))
                  :left (- (.-clientX event-position) (current-position :left))}
          new-position (fn [mouse]
                         {:top (- (mouse :top) (offset :top))
                          :left (- (mouse :left) (offset :left))})]
      (om/set-state! owner :new-position new-position)
      (om/update! cursor :dragging true)
      (tap mouse-mult user-movement))))

(defn touch-start
  [e cursor owner current-position]
  (move-start (aget (.-changedTouches e) 0) cursor owner current-position))

(defn mouse-start
  [e cursor owner current-position]
  (move-start e cursor owner current-position))

(defn move-end
  [cursor owner position-cursor]
  (let [user-movement (om/get-state owner :user-movement)]
    (untap mouse-mult user-movement)
    (om/update! cursor position-cursor (om/get-state owner :position))
    (om/update! cursor :dragging false)
    (om/set-state! owner :new-position nil)))

(defn position
  [item position-cursor state]
  (if (state :new-position)
    (state :position)
    (get-in item position-cursor)))

(defn draggable-item
  [view position-cursor]
  (fn [item owner]
    (reify
      om/IInitState
      (init-state [_]
        {:position (get-in item position-cursor)
         :user-movement (chan)
         :draggable (chan)
         :disabled false
         :new-position nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :user-movement)]
          (go (while true
            (let [mouse (<! position)
                  new-position (om/get-state owner :new-position)]
              (om/set-state! owner :position (new-position mouse)))))
          (go (while true
              (let [draggable (om/get-state owner :draggable)
                    disabled (not (<! draggable))]
                (om/set-state! owner :disabled disabled)
                (when disabled (move-end owner item position-cursor)))))))
      om/IRenderState
      (render-state [_ state]
        (let [current-position (position item position-cursor state)
              primative-value #(if (om/cursor? %) @% %)]
          (dom/div (clj->js {:style (conj {:position "fixed" :zIndex 800} current-position)
                             :className (when (get item :dragging) "dragging")
                             :onTouchStart #(touch-start % item owner (primative-value current-position))
                             :onMouseDown #(mouse-start % item owner (primative-value current-position))
                             :onTouchEnd #(move-end item owner position-cursor)
                             :onMouseUp #(move-end item owner position-cursor)})
                   (om/build view item {:init-state {:draggable (om/get-state owner :draggable)}})))))))
