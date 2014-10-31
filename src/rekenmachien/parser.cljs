(ns rekenmachien.parser
  (:require [clojure.walk :refer [prewalk]]))

(defn- replace-last [arr val]
  (assoc arr (dec (count arr)) val))

(defn- reduce-decimals [tokens]
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

(defn- reduce-infix [tokens oper]
  (when (seq tokens)
    (if (= (second tokens) oper)
      (reduce-infix (into [[oper (first tokens) (nth tokens 2)]]
                          (drop 3 tokens)) oper)
      (into [(first tokens)]
            (reduce-infix (next tokens) oper)))))

(defn- reduce-infixes [tokens] ; Het Mannetje Won Van De Oude Aap
  (prewalk (fn [x]
             (if (sequential? x)
               (reduce reduce-infix x [:pow :mul :div :add :sub])
               x))
           tokens))

(def openers #{:open :sin :cos :tan :asin :acos :atan :sqrt})

(defn- find-closing [tokens n]
  (if (seq tokens)
    (cond
     (and (= (first tokens) :close) (= n 0))
     0

     (openers (first tokens))
     (inc (find-closing (next tokens) (inc n)))

     (= (first tokens) :close)
     (inc (find-closing (next tokens) (dec n)))

     :else
     (inc (find-closing (next tokens) n)))
    0))

(defn- reduce-blocks [tokens]
  (when (seq tokens)
    (if (openers (first tokens))
     (let [pos (find-closing (next tokens) 0)]
       (into [(into [(first tokens)]
                    (reduce-blocks (take pos (next tokens))))]
             (reduce-blocks (next (drop pos (next tokens))))))
     (into [(first tokens)] (reduce-blocks (next tokens))))))

(defn parse [tokens]
  (-> tokens
      reduce-decimals
      reduce-blocks
      reduce-infixes))
