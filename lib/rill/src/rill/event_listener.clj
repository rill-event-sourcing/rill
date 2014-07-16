(ns rill.event-listener
  (:require [rill.event-store :as store]))

(def long-poll-seconds 20)

(defn listener
  [event-store stream-id version callback]
  (loop [version version]
    (recur (reduce (fn [v e]
                     (callback e)
                     (inc v))
                   (store/retrieve-events-since event-store stream-id version long-poll-seconds)))))

