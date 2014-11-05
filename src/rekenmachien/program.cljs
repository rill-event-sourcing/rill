(ns rekenmachien.program
  (:refer-clojure :exclude [empty])
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
    :abc "/",
    :x10y "×10^", :sqrt "√(", :x2 [:sup "2"], :pow "^", :x1 [:sup "-1"],
    :pi "π", :dot ",", :neg [:small "-"], :ans "Ans",
    :mul "×", :div "÷", :add "+", :sub "-",
    :open "(", :close ")"}
   token
   (str token)))

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

(defn del [{:keys [tokens cursor] :as program}]
  (no-clear-on-insert
   (if (> cursor 0)
     (-> program
         left
         (assoc :tokens (into [] (concat (take (dec cursor) tokens)
                                         (drop cursor tokens)))))
     program)))

(def post-or-infix-oper? #{:pow :x10y :mul :div :add :sub :x1 :x2 :abc})

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
  {:add math/add
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
        res (if (< -9 exp 1) (.replace (.toFixed val precision) #"\.?0*$" "") res)
        res (if (> exp 9) (.toExponential val 10) res)
        res (.replace res "." ",")
        res (.replace res #",?0*e\+?" "×10^")]
    res))

(defn finite? [val]
  (and (number? val)
       (not (or (.isNaN js/window val)
                (= js/Infinity val)
                (= (* -1 js/Infinity) val)))))

(defn run [{:keys [tokens]}]
  (try
    (let [val (calc (parser/parse tokens))]
      (if (finite? val)
        (do
          (reset! previous-result-atom val)
          (render-result val))
        "MATH ERROR"))
    (catch js/Object ex (str "SYNTAX ERROR" " " ex))))
