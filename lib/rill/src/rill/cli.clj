(ns rill.cli
  (:require [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.event-store.atom-store.event :refer [unprocessable?]]
            [clojure.core.async :refer [<!!]])
  (:gen-class))

(defn -main [url]
  {:pre [url]}
  (let [ch (event-channel (psql-event-store url)
                          all-events-stream-id -1 0)]
    (println "Printing all events...")
    (loop []
      (if-let [e (<!! ch)]
        (do (if (unprocessable? e)
              (println "Skipped message.")
              (prn e))
            (recur))))))


