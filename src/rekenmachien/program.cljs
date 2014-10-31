(ns rekenmachien.program
  (:refer-clojure :exclude [empty])
  (:require [rekenmachien.parser :as parser]))

(def empty {:tokens [], :cursor 0, :ins true})

(defn token-labels [token]
  (get
   {:sin "sin(" :cos "cos(" :tan "tan("
    :abc "/" :x10x [:sub "10"] :sqrt "√(" :x2 [:sup "2"] :pow "^" :x1 [:sup "-1"] :pi "π"
    :dot "," :neg [:small "-"] :ans "Ans" :mul "×" :div "÷" :add "+" :sub "-" :open "(" :close ")"}
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

(defn toggle-ins-mode [program]
  (update-in program [:ins] not))

(defn ins-mode? [{:keys [ins]}]
  ins)

(defn del [{:keys [tokens cursor] :as program}]
  (cond
   (< cursor (count tokens))
   (-> program
       (assoc :tokens (into [] (concat (take cursor tokens) (drop (inc cursor) tokens)))))

   (> cursor 0)
   (-> program
       (update-in [:cursor] dec)
       (assoc :tokens (into [] (take (dec cursor) tokens))))))

(defn backsp [{:keys [tokens cursor] :as program}]
  (if (> cursor 0)
    (-> program
        (update-in [:cursor] dec)
        (assoc :tokens (into [] (concat (take (dec cursor) tokens) (drop cursor tokens)))))
    program))

(defn insert [{:keys [tokens cursor ins] :as program} val]
  (right
   (if (or ins (= cursor (count tokens)))
     (assoc program :tokens (into [] (concat (take cursor tokens)
                                             [val]
                                             (drop cursor tokens))))
     (assoc program :tokens (into [] (concat (take cursor tokens)
                                             [val]
                                             (drop (inc cursor) tokens)))))))

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
