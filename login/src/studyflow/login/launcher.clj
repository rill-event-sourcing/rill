(ns studyflow.login.launcher
  (:gen-class)
  (:require
    [studyflow.login.main :refer [app]]
    [ring.adapter.jetty :as jetty])
)

(defn -main [& args]
   (jetty/run-jetty app {:port 3000 :join? false})) 
