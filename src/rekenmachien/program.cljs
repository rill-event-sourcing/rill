(ns rekenmachien.program
  (:refer-clojure :exclude [empty])
  (:require [rekenmachien.parser :as parser]))

(def empty {:tokens [], :cursor 0})

(defn label [token]
  (get
   {:sin "sin(", :cos "cos(", :tan "tan(",
    :asin [:span "sin" [:sup "-1"] "("],
    :acos [:span "cos" [:sup "-1"] "("],
    :atan [:span "tan" [:sup "-1"] "("],
    :abc "/",
    :x10y "×10^", :sqrt "√(", :x2 [:sup "2"], :pow "^", :x1 [:sup "-1"],
    :pi "π", :dot ",", :neg [:small "-"], :ans "Ans",
    :mul "×", :div "÷", :add "+", :sub "-",
    :open "(", :close ")"}
   token
   (str token)))

(defn left [{:keys [cursor] :as program}]
  (if (> cursor 0)
    (update-in program [:cursor] dec)
    program))

(defn right [{:keys [tokens cursor] :as program}]
  (if (< cursor (count tokens))
    (update-in program [:cursor] inc)
    program))

(defn del [{:keys [tokens cursor] :as program}]
  (if (> cursor 0)
    (-> program
        left
        (assoc :tokens (into [] (concat (take (dec cursor) tokens)
                                        (drop cursor tokens)))))
    program))

(defn insert [{:keys [tokens cursor] :as program} val]
  (right
   (assoc program :tokens (into [] (concat (take cursor tokens)
                                           [val]
                                           (drop cursor tokens))))))

(def keyword->math
  {:add +
   :sub -
   :mul *
   :div /
   :neg #(* % -1)
   :x1 #(.pow js/Math %1 -1)
   :x2 #(.pow js/Math %1 2)
   :pow #(.pow js/Math %1 %2)
   :x10y #(* %1 (.pow js/Math 10 %2))
   :sin #(.sin js/Math %)
   :cos #(.cos js/Math %)
   :tan #(.tan js/Math %)
   :asin #(.asin js/Math %)
   :acos #(.acos js/Math %)
   :atan #(.atan js/Math %)
   :sqrt #(.sqrt js/Math %)
   :open identity})

(defn calc [ast]
  (cond
   (= :pi ast)
   (.-PI js/Math)

   (number? ast)
   ast

   (sequential? ast)
   (apply (keyword->math (first ast))
          (map calc (next ast)))

   :else (throw (js/Error. (str "syntax error: " (pr-str ast))))))

(def precision 10)
(def max-digits (+ precision 2))

(defn- take-exp [s]
  (js/parseInt (or (last (re-find #"e(.*)" s)) "0")))

(defn render-result [val]
  (let [val (js/parseFloat (.toPrecision val precision))
        exp (js/parseInt (or (last (re-find #"e(.*)" (.toExponential val 10))) "0"))
        res (if (< -0.01 val 0.01) (.toExponential val 10) (str val))
        res (if (< -9 exp -1)
              (.replace (.toFixed val precision) #",?0*$" "")
              res)
        res (if (> exp 9) (.toExponential val 10) res)
        res (.replace res "." ",")
        res (.replace res #",?0*e\+?" "×10^")]
    res))

(defn run [{:keys [tokens]}]
  (try
    (render-result (calc (parser/parse tokens)))
    (catch js/Object ex "SYNTAX ERROR")))
