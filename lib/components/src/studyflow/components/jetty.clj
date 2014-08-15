(ns studyflow.components.jetty
  (:require [clojure.tools.logging :refer [info]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [ring.adapter.jetty :as jetty]
            [studyflow.components.uncaught-exception-handler :refer [wrap-uncaught-exception]]))

(defrecord JettyComponent [port ring-handler]
  Lifecycle
  (start [component]
    (info "Starting jetty on port: " port)
    (let [handler (wrap-uncaught-exception (:handler ring-handler))
          jetty (jetty/run-jetty handler {:port port
                                          :join? false})]
      (assoc component :jetty jetty)))
  (stop [component]
    (info "Stopping jetty")
    (when-let [jetty (:jetty component)]
      (when-not (.isStopped jetty)
        (.stop jetty)))
    component))

(defn jetty-component [port]
  (map->JettyComponent {:port port}))
