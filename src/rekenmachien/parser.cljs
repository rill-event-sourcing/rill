(ns rekenmachien.parser
  (:require [clojure.walk :refer [postwalk]]))

(def block-oper? #{:open :sin :cos :tan :asin :acos :atan :sqrt})

(defn- find-closing [tokens n]
  (if (seq tokens)
    (cond
     (and (= (first tokens) :close) (= n 0))
     0

     (block-oper? (first tokens))
     (inc (find-closing (next tokens) (inc n)))

     (= (first tokens) :close)
     (inc (find-closing (next tokens) (dec n)))

     :else
     (inc (find-closing (next tokens) n)))
    0))

(defn parse-blocks [tokens]
  (when (seq tokens)
    (if (block-oper? (first tokens))
     (let [pos (find-closing (next tokens) 0)]
       (into [(into [(first tokens)]
                    (parse-blocks (take pos (next tokens))))]
             (parse-blocks (next (drop pos (next tokens))))))
     (into [(first tokens)] (parse-blocks (next tokens))))))

(defn parse-prefix [tokens oper?]
  (when (seq tokens)
    (if (oper? (first tokens))
      (parse-prefix (into [[(first tokens) (second tokens)]] (drop 2 tokens)) oper?)
      (into [(first tokens)] (parse-prefix (next tokens) oper?)))))

(defn parse-infix [tokens oper]
  (when (seq tokens)
    (if (= (second tokens) oper)
      (parse-infix (into [[oper (first tokens) (nth tokens 2)]] (drop 3 tokens)) oper)
      (into [(first tokens)] (parse-infix (next tokens) oper)))))

(defn parse-postfix [tokens oper?]
  (when (seq tokens)
    (if (oper? (second tokens))
      (parse-postfix (into [[(second tokens) (first tokens)]] (drop 2 tokens)) oper?)
      (into [(first tokens)] (parse-postfix (next tokens) oper?)))))

(defn parse-opers [tokens] ; Het Mannetje Won Van De Oude Aap
  (postwalk (fn [x]
              (if (sequential? x)
                (reduce parse-infix
                        (parse-prefix
                         (parse-postfix x #{:x1 :x2})
                         #{:neg})
                        [:pow :x10y :mul :div :add :sub])
                x))
            tokens))

(defn- replace-last [arr val]
  (assoc arr (dec (count arr)) val))

(defn parse-decimals [tokens]
  (map
   #(if (string? %) (js/parseFloat %) %)
   (reduce (fn [result token]
             (let [token (if (= :dot token) "." token)]
               (if (or (number? token) (= "." token))
                 (if (string? (last result))
                   (replace-last result (str (last result) token))
                   (conj result (str token)))
                 (conj result token))))
           []
           tokens)))

(defn parse [tokens]
  (let [ast (-> tokens
                parse-decimals
                parse-blocks
                parse-opers)]
    (when (> (count ast) 1) (throw (js/Error. "too many calculations")))
    (first ast)))
