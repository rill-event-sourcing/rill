(ns studyflow.school-administration.dev
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]))

(defn bootstrap!
  []
  (println "Starting repl at port 7888")
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler))

