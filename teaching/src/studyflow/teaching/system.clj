(ns studyflow.teaching.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.teaching.system.components.read-model :refer [read-model-component]]
            [studyflow.teaching.system.components.ring-handler :refer [ring-handler-component]]))

(defn prod-system [config-options]
  (log/info "Running the teaching production system")
  (let [{:keys [port event-store-config redirect-urls session-store-url cookie-domain]} config-options]
    (component/system-map
     :config-options config-options
     :ring-handler (component/using
                    (ring-handler-component redirect-urls cookie-domain)
                    [:event-store :read-model :session-store])
     :jetty (component/using
             (jetty-component port)
             [:ring-handler])
     :event-channel (component/using
                     (event-channel-component 1)
                     [:event-store])
     :event-store (component/using
                   (psql-event-store-component event-store-config)
                   [])
     :read-model (component/using
                  (read-model-component 0)
                  [:event-store :event-channel])
     :session-store (redis-session-store {:uri session-store-url})
     :uncaught-exception-handler (component/using
                                  (uncaught-exception-handler-component)
                                  []))))
