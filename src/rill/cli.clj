(ns rill.cli
  (:require [rill.event-store.atom-store :refer [atom-event-store]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.event-store.atom-store.event :refer [unprocessable?]]
            [clojure.core.async :refer [<!!]]))

(defn -main [& args]
  (let [ch (event-channel (atom-event-store "http://127.0.0.1:2113" {:user "admin" :password "changeit"})
                          all-events-stream-id -1 0)]
    (println "Printing all events...")
    (loop []
      (if-let [e (<!! ch)]
        (do (if (unprocessable? e)
              (println "Skipped message.")
              (prn e))
            (recur))))))


