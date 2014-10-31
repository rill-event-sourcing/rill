(ns rekenmachien.program
  (:refer-clojure :exclude [empty])
  (:require [rekenmachien.parser :as parser]))

(def empty {:tokens [], :cursor 0})

(defn token-labels [token]
  (get
   {:sin "sin(", :cos "cos(", :tan "tan(",
    :asin [:span "sin" [:sup "-1"] "("],
    :acos [:span "cos" [:sup "-1"] "("],
    :atan [:span "tan" [:sup "-1"] "("],
    :abc "/",
    :x10x [:sub "10"], :sqrt "√(", :x2 [:sup "2"], :pow "^", :x1 [:sup "-1"],
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
        (update-in [:cursor] dec)
        (assoc :tokens (into [] (concat (take (dec cursor) tokens) (drop cursor tokens)))))
    program))

(defn insert [{:keys [tokens cursor] :as program} val]
  (right
   (assoc program :tokens (into [] (concat (take cursor tokens)
                                           [val]
                                           (drop cursor tokens))))))

(defn round-for-display [val]
  (js/parseFloat (.toPrecision val 10)))

(defn calc [ast]
  (cond
   (= :pi ast)
   (.-PI js/Math)

   (number? ast)
   ast

   (sequential? ast)
   (let [oper (first ast)
         args (map calc (next ast))]
     (case (first ast)
       :add (apply + args)
       :sub (apply - args)
       :mul (apply * args)
       :div (apply / args)
       :cos (.cos js/Math (first args))
       :sin (.sin js/Math (first args))
       :tan (.tan js/Math (first args))
       :open (calc (first (rest ast)))))

   :else (throw (js/Error. "syntax error"))))

(defn run [{:keys [tokens]}]
  (try
    (let [ast (parser/parse tokens)]
      (round-for-display (calc (first ast))))
    (catch js/Object ex "SYNTAX ERROR")))
