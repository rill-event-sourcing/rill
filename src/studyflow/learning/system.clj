(ns studyflow.learning.system
  (:require [com.stuartsierra.component :as component :refer [using]]
            [studyflow.web.durable-session-store :refer [durable-store]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.learning.system.components.cache-heater :refer [cache-heater]]
            [studyflow.learning.system.components.publishing-api :refer [publishing-api-component]]
            [studyflow.learning.system.components.read-model :refer [read-model-component]]
            [studyflow.learning.system.components.ring-handler :refer [ring-handler-component]]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]]
            [studyflow.web.app-status :refer [app-status-component]]
            [clojure.tools.logging :as log]))

(defn prod-system [config-options]
  (log/info "Running the production system")
  (let [{:keys [port event-store-config internal-api-port redirect-urls session-store-url cookie-domain]} config-options]
    (component/system-map
     :config-options config-options
     :publishing-api-handler (-> (publishing-api-component)
                                 (using [:event-store]))
     :publishing-api-jetty (-> (jetty-component internal-api-port)
                               (using {:ring-handler :publishing-api-handler
                                       :app-status-component :app-status-component}))
     :session-store (durable-store session-store-url)
     :ring-handler (-> (ring-handler-component redirect-urls cookie-domain)
                       (using [:event-store :read-model :session-store :cache-heater]))
     :cache-heater (-> (cache-heater 2)
                       (using [:event-channel :event-store]))
     :app-status-component (-> (app-status-component 1)
                               (using [:event-channel]))
     :jetty (-> (jetty-component port)
                (using [:ring-handler :app-status-component]))
     :event-channel (-> (event-channel-component 3)
                        (using [:event-store]))
     :event-store (psql-event-store-component event-store-config)
     :read-model (-> (read-model-component 0)
                     (using [:event-store :event-channel]))
     :uncaught-exception-handler (uncaught-exception-handler-component))))
