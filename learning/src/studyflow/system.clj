(ns studyflow.system
  (:require [com.stuartsierra.component :as component]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.system.components.publishing-api :refer [publishing-api-component]]
            [studyflow.system.components.read-model :refer [read-model-component]]
            [studyflow.system.components.ring-handler :refer [ring-handler-component]]
            [clojure.tools.logging :as log]))

(defn prod-system [config-options]
  (log/info "Running the production system")
  (let [{:keys [port event-store-config internal-api-port session-store-config redirect-urls]} config-options]
    (component/system-map
     :config-options config-options
     :publishing-api-handler (component/using
                              (publishing-api-component)
                              [:event-store])
     :publishing-api-jetty (component/using
                            (jetty-component internal-api-port)
                            [:publishing-api-handler])
     :session-store (redis-session-store session-store-config)
     :ring-handler (component/using
                    (ring-handler-component redirect-urls)
                    [:event-store :read-model :session-store])
     :jetty (component/using
             (jetty-component port)
             [:ring-handler])
     :event-channel (component/using
                     (event-channel-component)
                     [:event-store])
     :event-store (component/using
                   (atom-event-store-component event-store-config)
                   [])
     :read-model (component/using
                  (read-model-component)
                  [:event-store :event-channel]))))
