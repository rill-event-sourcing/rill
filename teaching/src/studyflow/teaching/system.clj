(ns studyflow.teaching.system
  (:require [clojure.tools.logging :as log]
            [studyflow.web.durable-session-store :refer [durable-store]]
            [com.stuartsierra.component :as component :refer [using]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.web.app-status :refer [app-status-component]]
            [studyflow.teaching.system.components.read-model :refer [read-model-component]]
            [studyflow.teaching.system.components.ring-handler :refer [ring-handler-component]]))

(defn prod-system [config-options]
  (log/info "Running the teaching production system")
  (let [{:keys [port event-store-config redirect-urls session-store-url cookie-domain]} config-options]
    (component/system-map
     :config-options config-options
     :ring-handler (-> (ring-handler-component redirect-urls)
                       (using [:event-store :read-model :session-store]))
     :session-store (durable-store session-store-url)
     :jetty (-> (jetty-component port)
                (using [:ring-handler :app-status-component]))
     :app-status-component (-> (app-status-component 1)
                               (using [:event-channel]))
     :event-channel (-> (event-channel-component 2)
                        (using [:event-store]))
     :event-store (psql-event-store-component event-store-config)
     :read-model (-> (read-model-component 0)
                     (using [:event-store :event-channel]))
     :uncaught-exception-handler (uncaught-exception-handler-component))))

