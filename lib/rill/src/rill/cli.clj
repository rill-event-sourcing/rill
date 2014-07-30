(ns rill.cli
  (:require [rill.event-store.atom-store :refer [atom-event-store]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.event-store.atom-store.event :refer [unprocessable?]]
            [clojure.core.async :refer [<!!]])
  (:gen-class))

(defn -main [url user password]
  {:pre [url user password]}
  (let [ch (event-channel (atom-event-store url {:user user :password password})
                          all-events-stream-id -1 0)]
    (println "Printing all events...")
    (loop []
      (if-let [e (<!! ch)]
        (do (if (unprocessable? e)
              (println "Skipped message.")
              (prn e))
            (recur))))))


