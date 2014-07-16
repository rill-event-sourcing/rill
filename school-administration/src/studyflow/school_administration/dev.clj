(ns studyflow.school-administration.dev
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [studyflow.school-administration.main :as main]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [cider.nrepl :refer (cider-nrepl-handler)]))

(defn bootstrap!
  []
  (println "Starting repl at port 7888")
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (main/event-listener (event-channel main/event-store all-events-stream-id -1 0) main/my-read-model))
