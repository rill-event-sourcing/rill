(ns studyflow.login.launcher
  (:gen-class)
  (:require [studyflow.login.system :as system]
            [com.stuartsierra.component :as component]))

(defn -main [& args]
  (let [system (system/make-system {:jetty-port 4000})]
    (component/start system)))
