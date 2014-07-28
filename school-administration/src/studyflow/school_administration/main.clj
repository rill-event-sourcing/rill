(ns studyflow.school-administration.main
  (:require [clojure.tools.logging :as log]
            [studyflow.school-administration.system :as system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main [& args]
  (log/info "Main Studyflow learning app")
  (let [s (-> (system/prod-system system/prod-config)
              component/start)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (component/stop s)
                                 (log/info "Stopping is done, bye"))))))
