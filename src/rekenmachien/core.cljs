(ns rekenmachien.core
  (:require [clojure.string :as s]
            [rekenmachien.program :as program]
            [reagent.core :as reagent :refer [atom]]))

(defonce program-atom (atom program/empty))
(defonce inv-mode-atom (atom false))
(defonce cursor-location-atom (atom 0))
(defonce result-atom (atom nil))

(def button-labels
  {:inv "INV", :dec "f↔d"
   :sin "sin", :cos "cos", :tan "tan",
   :asin [:span "sin" [:sup "-1"]],
   :acos [:span "cos" [:sup "-1"]],
   :atan [:span "tan" [:sup "-1"]],
   :abc [:span "a" [:sup "b"] "/" [:sub "c"]],
   :x10y [:span "x10" [:sup "y"]], :sqrt "√", :x2 "x²", :pow "^", :x1 "x⁻¹",
   :left "⇐", :right "⇒", :del "DEL", :clear "C",
   :pi "π", :dot ",", :neg "(-)", :ans "ANS",
   :mul "×", :div "÷", :add "+", :sub "−",
   :open "(", :close ")", :show "="})

(defn button-press! [val]
  (case val
    :inv (swap! inv-mode-atom not)

    :left (swap! program-atom program/left)
    :right (swap! program-atom program/right)
    :del (swap! program-atom program/del)
    :backsp (swap! program-atom program/backsp)
    :clear (do
             (reset! program-atom program/empty)
             (reset! result-atom nil))

    :show (swap! result-atom #(program/run @program-atom))

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
    [program-component]
    [result-component]]
   [:div.keyboard
    [:section.inv
     (for [button (if @inv-mode-atom
                    [:sin :cos :tan :abc]
                    [:asin :acos :atan :dec])]
       [:label [:span.label (get button-labels button)]])]
    (for [[section rows] [[:functions [(if @inv-mode-atom
                                         [:asin :acos :atan :dec :x10y]
                                         [:sin :cos :tan :abc :x10y])
                                       [:sqrt :x2 :pow :x1 :pi]]]
                          [:number-oper [[:inv :left :right :open :close]
                                         [7 8 9 :del :clear]
                                         [4 5 6 :mul :div]
                                         [1 2 3 :add :sub]
                                         [0 :dot :neg :ans :show]]]]]
      [:section {:key section :class section}
       (for [row rows]
         [:div.row
          (for [button row]
            [:button {:type "button" :on-click #(button-press! button)
                      :class (if (keyword? button) (name button) "digit")}
             [:span.label (get button-labels button (str button))]])])])]
   [:style {:type "text/css"}
    (str
     ".rekenmachien { background: #888; padding: 1em; width: calc((4.5em * 5) + 2em); border-radius: .5em; }"
     ".rekenmachien, .rekenmachien * { box-sizing: border-box; font-size: 14px; font-family: sans-serif; }"
     ".rekenmachien .display { position: relative; width: calc((4.5em * 5)); height: 5em; background: #bc6; border: solid #000 2px; border-radius: .5em; overflow: hidden; }"
     ".rekenmachien .display .program { padding: .5em .5em .25em .5em; background: #ab5; } "
     ".rekenmachien sup, .rekenmachien sub { font-size: 66%; }"
     ".rekenmachien .display .program small { font-size: 75%; }"
     ".rekenmachien .display .program span { display: inline-block; text-align: center; } "
     ".rekenmachien .display .program span.placeholder { width: .5em; } "
     ".rekenmachien .display .program .with-cursor { border-left: 1px solid #000; } "
     ".rekenmachien .display .result { font-size: 150%; position: absolute; bottom: .25em; right: .5em;}"
     ".rekenmachien button, .rekenmachien section.inv label { width: 4em; height: 3em; margin: .25em; padding: 0; display: inline-block; }"
     ".rekenmachien button { color: #000; background: #ddd; font-weight: bold; border-radius: .5em; border: solid #aaa 2px; }"
     ".rekenmachien button .label { font-size: 125%; } "
     ".rekenmachien button.digit { color: #fff; background: #444; } "
     ".rekenmachien button.digit .label { font-size: 150%; }"
     ".rekenmachien button.inv { background: #fc0; } "
     ".rekenmachien button.show { color: #fff; background: #00f; } "
     ".rekenmachien section.inv label { text-align: center; vertical-align: bottom; color: #fc0; height: 1em; }"
     ".rekenmachien section.inv label span.label { font-size: 75%; }"
     ".rekenmachien section.inv label span.label span { font-size: inherit; }"
     ".rekenmachien button.del, .rekenmachien button.clear { color: #fff; background: #c00; } "
     ".rekenmachien .keyboard section { display: inline-block; vertical-align: top; margin: 0; }")]])

(def key-code->button
  {:normal {8 :del
            13 :show
            37 :left
            39 :right
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
