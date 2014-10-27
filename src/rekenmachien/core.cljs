(ns rekenmachien.core
  (:require [clojure.string :as s]
            [rekenmachien.program :as program]
            [reagent.core :as reagent :refer [atom]]))

(defonce program-atom (atom program/empty))
(defonce inv-mode-atom (atom false))
(defonce cursor-location-atom (atom 0))
(defonce result-atom (atom nil))

(def button-labels
  {:inv "INV"
   :sin "sin" :cos "cos" :tan "tan"
   :abc [:span "a" [:sup "b"] "/" [:sub "c"]] :x10x [:span "x10" [:sup "x"]] :sqrt "√" :x2 "x²" :pow "^" :x1 "x⁻¹" :pi "π"
   :left "←" :right "→" :ins "INS" :del "DEL" :clear "C"
   :dot "." :neg "(-)" :ans "ANS" :mul "×" :div "/" :add "+" :sub "-" :open "(" :close ")" :show "="})

(defn button-press! [val]
  (case val
    :inv (swap! inv-mode-atom not)

    :left (swap! program-atom program/left)
    :right (swap! program-atom program/right)
    :ins (swap! program-atom program/toggle-ins-mode)
    :del (swap! program-atom program/del)
    :clear (reset! program-atom program/empty)

    :show (swap! result-atom #(rand))

    ;; otherwise
    (swap! program-atom program/insert val)))

(defn program-component []
  (let [{:keys [cursor tokens]} @program-atom]
    [:div.program
     (map (fn [token loc]
            [:span
             (if (= loc cursor) {:class "with-cursor"})
             (program/token-labels token)])
          tokens
          (iterate inc 0))
     (when (>= cursor (count tokens))
       [:span.placeholder.with-cursor " "])]))

(defn result-component []
  (let [result @result-atom]
    [:div.result result]))

(defn main-component []
  [:div.rekenmachien
   [:div.display
    (when @inv-mode-atom [:span.inv-mode "i"])
    (when (program/ins-mode? @program-atom) [:span.ins-mode "ins"])
    [program-component]
    [result-component]]
   [:div.keyboard
    (for [[section rows] [[:functions [[:inv]
                                       [:sin :cos :tan :abc :x10x]
                                       [:sqrt :x2 :pow :x1 :pi]]]
                          [:number-oper [[:left :right :ins :del :clear]
                                         [7 8 9 :mul :div]
                                         [4 5 6 :add :sub]
                                         [1 2 3 :open :close]
                                         [0 :dot :neg :ans :show]]]]]
      [:section {:key section :class section}
       (for [row rows]
         [:div.row
          (for [button row]
            [:button {:type "button" :on-click #(button-press! button)
                      :class (if (keyword? button) (name button) (str button))}
             [:span.label (get button-labels button (str button))]])])])]
   [:style {:type "text/css"}
    (str
     ".rekenmachien { background: #ccc; padding: 1em; width: calc((3.5em * 10) + 6em); border-radius: 1em; }"
     ".rekenmachien, .rekenmachien * { box-sizing: border-box; font-size: 14px; font-family: sans-serif; }"
     ".rekenmachien .display { position: relative; width: calc((3.5em * 10) + 4em); height: 5em; background: #ddd; border-radius: .5em; overflow: hidden; }"
     ".rekenmachien .display .inv-mode, .rekenmachien .display .ins-mode { position: absolute; top: .5em; font-size: 66%; background: black; color: #eee; padding: .1em .25em; border-radius: .3em; }"
     ".rekenmachien .display .inv-mode { left: .5em; }"
     ".rekenmachien .display .ins-mode { right: .5em; }"
     ".rekenmachien .display .program { padding: .5em 1.5em .25em 1.5em; background: #eee; } "
     ".rekenmachien .display .program sup, .rekenmachien .display .program sub { font-size: 66%; }"
     ".rekenmachien .display .program small { font-size: 75%; }"
     ".rekenmachien .display .program span { display: inline-block; text-align: center; } "
     ".rekenmachien .display .program span.placeholder { width: .5em; } "
     ".rekenmachien .display .program .with-cursor { border-bottom: 2px solid #000; } "
     ".rekenmachien .display .result { font-size: 175%; font-weight: bold; position: absolute; bottom: .25em; right: 1em;}"
     ".rekenmachien button { width: 3em; height: 3em; margin: .25em; }"
     ".rekenmachien .keyboard section { display: inline-block; vertical-align: top; margin: 1em; }")]])

(def key-code->button
  {:normal {8 :del
            13 :show
            37 :left
            39 :right
            45 :ins
            46 :del
            48 0, 49 1, 50 2, 51 3, 52 4, 53 5, 54 6, 55 7, 56 8, 57 9
            65 :show
            67 :cos
            83 :sin
            84 :tan
            106 :mul
            107 :add
            109 :sub
            110 :dot
            111 :div
            187 :show
            189 :sub
            190 :dot
            191 :div}
   :shifted {48 :close
             54 :pow
             56 :mul
             57 :open
             67 :clear
             187 :add}})

(defn main []
  (set! (.-onkeydown js/document)
        #(when-let [button (get-in key-code->button
                                   [(if (.-shiftKey %) :shifted :normal) (.-keyCode %)])]
           (.preventDefault %)
           (button-press! button)))
  (reagent/render-component [main-component] (.getElementById js/document "app")))
