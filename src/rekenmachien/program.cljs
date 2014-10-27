(ns rekenmachien.program
  (:refer-clojure :exclude [empty]))

(def empty {:tokens [], :cursor 0, :ins false})

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

(defn insert [{:keys [tokens cursor ins] :as program} val]
  (right
   (if (or ins (= cursor (count tokens)))
     (assoc program :tokens (into [] (concat (take cursor tokens)
                                             [val]
                                             (drop cursor tokens))))
     (assoc program :tokens (into [] (concat (take cursor tokens)
                                             [val]
                                             (drop (inc cursor) tokens)))))))
