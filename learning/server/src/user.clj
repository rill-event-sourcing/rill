(ns user
  (:require [studyflow.system :as sys]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]
            [clojure.core.async :refer [close! <!!]]))

(defonce web-server nil)

(defn start []
  (sys/init)
  (alter-var-root #'web-server (constantly (run-jetty #'sys/web-handler {:port 3000 :join? false}))))

(defn stop []
  (when web-server
    (.stop web-server))
  (close! sys/channel)
  (<!! sys/event-listener))

