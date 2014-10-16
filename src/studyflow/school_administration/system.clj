(ns studyflow.school-administration.system
  (:require [com.stuartsierra.component :as component :refer [using]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.web.app-status :refer [app-status-component]]
            [studyflow.school-administration.eduroute-listener :refer [eduroute-listener-component]]
            [studyflow.school-administration.system.components.read-model :refer [read-model-component]]
            [studyflow.school-administration.system.components.ring-handler :refer [ring-handler-component]]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]]
            [clojure.tools.logging :as log]))

(defn prod-system [config-options]
  (log/info "Running the production system")
  (let [{:keys [port event-store-config secure-site-defaults?]} config-options]
    (component/system-map
     :config-options config-options
     :ring-handler (-> (ring-handler-component secure-site-defaults?)
                       (using [:event-store :read-model]))
     :jetty (-> (jetty-component port)
                (using [:ring-handler :app-status-component]))
     :app-status-component (-> (app-status-component 2)
                               (using [:event-channel]))
     :event-channel (-> (event-channel-component 3)
                        (using [:event-store]))
     :event-store (psql-event-store-component event-store-config)
     :read-model (-> (read-model-component 0)
                     (using [:event-store :event-channel]))
     :eduroute-listener (-> (eduroute-listener-component 1)
                            (using [:event-channel :event-store]))
     :uncaught-exception-handler (uncaught-exception-handler-component))))
