(ns rekenmachien.program
  (:refer-clojure :exclude [empty empty?])
  (:require [rekenmachien.math :as math]
            [rekenmachien.parser :as parser]))

(defonce empty {:tokens [], :cursor 0, :clear-on-insert false})
(defonce previous-result-atom (atom nil))

(defn label [token]
  (get
   {:sin "sin(", :cos "cos(", :tan "tan(",
    :asin [:span "sin" [:sup "-1"] "("],
    :acos [:span "cos" [:sup "-1"] "("],
    :atan [:span "tan" [:sup "-1"] "("],
    :frac "/",
    :x10y "×10^", :sqrt "√(", :x2 [:sup "2"], :pow "^", :x1 [:sup "-1"],
    :pi "π", :dot ",", :neg [:small "-"], :ans "Ans",
    :mul "×", :div "÷", :add "+", :sub "-",
    :open "(", :close ")"}
   token
   (str token)))

(defn empty? [{:keys [tokens]}]
  (not (seq tokens)))

(defn do-clear-on-insert [program]
  (assoc program :clear-on-insert true))

(defn no-clear-on-insert [program]
  (assoc program :clear-on-insert false))

(defn left [{:keys [cursor] :as program}]
  (no-clear-on-insert
   (if (> cursor 0)
     (update-in program [:cursor] dec)
     program)))

(defn right [{:keys [tokens cursor] :as program}]
  (no-clear-on-insert
   (if (< cursor (count tokens))
     (update-in program [:cursor] inc)
     program)))

(defn move-to [program loc]
  (no-clear-on-insert
   (assoc program :cursor loc)))

(defn del [{:keys [tokens cursor] :as program}]
  (no-clear-on-insert
   (if (> cursor 0)
     (-> program
         left
         (assoc :tokens (into [] (concat (take (dec cursor) tokens)
                                         (drop cursor tokens)))))
     program)))

(def post-or-infix-oper? #{:pow :frac :x10y :mul :div :add :sub :x1 :x2})

(defn insert [program val]
  (let [program (if (:clear-on-insert program)
                  (if (post-or-infix-oper? val)
                    (assoc empty :cursor 1 :tokens [:ans])
                    empty)
                  program)
        {:keys [tokens cursor]} program]
    (right
     (assoc program :tokens (into [] (concat (take cursor tokens)
                                             [val]
                                             (drop cursor tokens)))))))

(def keyword->math
  {:frac math/frac
   :add math/add
   :sub math/sub
   :mul math/mul
   :div math/div
   :neg math/neg
   :x1 math/x1
   :x2 math/x2
   :pow math/pow
   :x10y math/x10y
   :sin math/sin
   :cos math/cos
   :tan math/tan
   :asin math/asin
   :acos math/acos
   :atan math/atan
   :sqrt math/sqrt
   :open identity})

(defn calc [ast]
  (cond
   (= :pi ast)
   math/pi

   (= :ans ast)
   (or @previous-result-atom 0)

   (or (number? ast) (math/fraction? ast))
   ast

   (sequential? ast)
   (let [args (map calc (next ast))]
     (when-not (seq args) (throw :syntax-error))
     (apply (keyword->math (first ast)) args))

   :else (throw :syntax-error)))

(def precision 10)
(def max-digits (+ precision 2))

(defn- take-exp [s]
  (js/parseInt (or (last (re-find #"e(.*)" s)) "0")))

(defn render-result [val]
  (if (math/fraction? val)
    (str val)
    (let [val (js/parseFloat (.toPrecision val precision))
          exp (js/parseInt (or (last (re-find #"e(.*)" (.toExponential val 10))) "0"))
          res (if (< -0.01 val 0.01) (.toExponential val 10) (str val))
          res (if (< -9 exp 1) (.replace (.toFixed val precision) #"\.?0*$" "") res)
          res (if (> exp 9) (.toExponential val 10) res)
          res (.replace res "." ",")
          res (.replace res #",?0*e\+?" "×10^")]
      res)))

(defn run [{:keys [tokens]}]
  (try
    (let [val (calc (parser/parse tokens))]
      (when-not (or (math/finite? val) (math/fraction? val))
        (throw :math-error))
      (reset! previous-result-atom val)
      (render-result val))
    (catch js/Object ex
      (if (= :math-error ex) "MATH ERROR" "SYNTAX ERROR"))))
