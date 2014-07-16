(ns studyflow.system.components.jetty
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord JettyComponent [port ring-handler]
  Lifecycle
  (start [component]
    (info "Starting jetty on port: " port)
    (assoc component :jetty (jetty/run-jetty (:handler ring-handler) {:port port
                                                                      :join? false})))
  (stop [component]
    (info "Stopping jetty")
    (when-let [jetty (:jetty component)]
      (when-not (.isStopped jetty)
        (.stop jetty)))
    component))

(defn jetty-component [port]
  (map->JettyComponent {:port port}))
