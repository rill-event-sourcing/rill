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
             (str token)])
          tokens
          (iterate inc 0))
     (when (>= cursor (count tokens))
       [:span.with-cursor " "])]))

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
     ".rekenmachien .display .program span { display: inline-block; } "
     ".rekenmachien .display .program .with-cursor { border-bottom: 1px solid #000; min-width: .5em; } "
     ".rekenmachien .display .result { font-size: 175%; font-weight: bold; position: absolute; bottom: .25em; right: 1em;}"
     ".rekenmachien button { width: 3em; height: 3em; margin: .25em; }"
     ".rekenmachien .keyboard section { display: inline-block; vertical-align: top; margin: 1em; }")]])

(defn main []
  (reagent/render-component [main-component] (.getElementById js/document "app")))
