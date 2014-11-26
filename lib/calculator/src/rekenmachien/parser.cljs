(ns rekenmachien.parser
  (:require [clojure.walk :refer [postwalk]]))

(def oper? #{:x10y :add :sub :mul :div :neg :pow :frac})
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

(defn blocks [tokens]
  (when (seq tokens)
    (if (block-oper? (first tokens))
      (let [pos (find-closing (next tokens) 0)]
        (into [(into [(first tokens)]
                     (blocks (take pos (next tokens))))]
              (blocks (next (drop pos (next tokens))))))
      (into [(first tokens)] (blocks (next tokens))))))

(defn infix-walker [tokens oper?]
  (when (seq tokens)
    (if (oper? (second tokens))
      (if (= :neg (nth tokens 2))
        (infix-walker (into [[(second tokens) (first tokens) (nth tokens 2) (nth tokens 3)]] (drop 4 tokens)) oper?)
        (infix-walker (into [[(second tokens) (first tokens) (nth tokens 2)]] (drop 3 tokens)) oper?))
      (into [(first tokens)] (infix-walker (next tokens) oper?)))))

(def powers {:x1 [:pow -1] :x2 [:pow 2]})

(defn special-power-walker [tokens]
  (when (seq tokens)
    (if-let [oper (powers (first tokens))]
      (into oper (special-power-walker (next tokens)))
      (into [(first tokens)] (special-power-walker (next tokens))))))

(defn neg-oper [tokens]
  (when (seq tokens)
    (if (= (first tokens) :neg)
      (into [[:neg (second tokens)]] (neg-oper (drop 2 tokens)))
      (into [(first tokens)] (neg-oper (next tokens))))))

(defn dangling-subs [tokens]
  (when (seq tokens)
    (if (and (oper? (first tokens))
             (= :sub (second tokens)))
      (dangling-subs (into [(first tokens) :neg] (drop 2 tokens)))
      (into [(first tokens)] (dangling-subs (next tokens))))))

(defn squash-negs [tokens]
  (when (seq tokens)
    (if-let [negs (seq (take-while #(= :neg %) tokens))]
      (if (= 0 (rem (count negs) 2))
        (squash-negs (drop (count negs) tokens))
        (into [:neg] (squash-negs (drop (count negs) tokens))))
      (into [(first tokens)] (squash-negs (next tokens))))))

(defn infix [opers tokens]
  (postwalk #(if (sequential? %) (reduce infix-walker % opers) %) tokens))

(defn special-powers [tokens]
  (postwalk #(if (sequential? %) (special-power-walker %) %) tokens))

(defn negs [tokens]
  (postwalk #(if (sequential? %) (neg-oper %) %) tokens))

(defn- replace-last [arr val]
  (assoc arr (dec (count arr)) val))

(defn decimals [tokens]
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

(defn strip-helper-zero [tokens]
  (if (and (> (count tokens) 1)
           (= 0 (first tokens)))
    (next tokens)
    tokens))

(defn parse [tokens]
  (let [ast (->> tokens
                 (into [0])
                 decimals
                 dangling-subs
                 squash-negs
                 blocks
                 special-powers
                 (infix [#{:frac} #{:pow}])
                 negs
                 (infix [#{:x10y} #{:mul :div} #{:add :sub}])
                 strip-helper-zero)]
    (when (> (count ast) 1) (throw :syntax-error))
    (first ast)))
