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

(defn parse-infix [tokens oper]
  (when (seq tokens)
    (if (= (second tokens) oper)
      (parse-infix (into [[oper (first tokens) (nth tokens 2)]] (drop 3 tokens)) oper)
      (into [(first tokens)] (parse-infix (next tokens) oper)))))

(defn parse-special [tokens oper?]
  (when (seq tokens)
    (if-let [oper (oper? (first tokens))]
      (into oper (parse-special (next tokens) oper?))
      (into [(first tokens)] (parse-special (next tokens) oper?)))))

(defn parse-opers [tokens] ; Het Mannetje Won Van De Oude Aap
  (postwalk (fn [x]
              (if (sequential? x)
                (reduce parse-infix
                        (parse-special x {:neg [-1 :mul]
                                          :x1 [:pow -1]
                                          :x2 [:pow 2]})
                        [:frac :pow :x10y :mul :div :add :sub])
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
    (when (> (count ast) 1) (throw :syntax-error))
    (first ast)))
