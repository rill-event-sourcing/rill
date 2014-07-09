(ns studyflow.main
  (:require [clojure.tools.logging :refer [info]]
            [studyflow.system :as system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main [& args]
  (info "Main Studyflow learning app")
  (let [s (-> (system/prod-system system/prod-config)
              component/start)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (component/stop s)
                                 (info "Stopping is done, bye"))))))
