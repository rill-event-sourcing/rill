(ns studyflow.login.launcher
  (:gen-class)
  (:require [studyflow.login.system :refer [init start]]))

(defn -main [& args]
  (init {:jetty-port 3000})
  (start))
