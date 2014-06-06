(ns user
  (:require [studyflow.system :as sys]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.test :as test :refer [run-all-tests]]))

(defonce web-server nil)

(defn start []
  (sys/init)
  (alter-var-root #'web-server (constantly (run-jetty #'sys/web-handler {:port 3000 :join? false}))))

(defn stop []
  (.stop web-server))
