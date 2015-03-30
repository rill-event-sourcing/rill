(ns todo.core
  (:require [rill.event-store.memory :refer [memory-store]]
            [rill.repository :refer [wrap-basic-repository]]
            [ring.adapter.jetty :refer [run-jetty]]
            [todo.task :as task]
            [todo.web :as web])
  (:import [java.util UUID]))

(defonce store (wrap-basic-repository (memory-store)))

(defn -main []
  (task/setup! store)
  (run-jetty web/handler {:port 8080 :join? false}))
