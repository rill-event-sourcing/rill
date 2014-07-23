(ns studyflow.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [studyflow.system.components.event-channel :refer [event-channel-component]]
            [studyflow.system.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.system.components.internal-api :refer [internal-api-component]]
            [studyflow.system.components.jetty :refer [jetty-component]]
            [studyflow.system.components.read-model :refer [read-model-component]]
            [studyflow.system.components.ring-handler :refer [ring-handler-component]]
            [studyflow.system.components.session-store :refer [redis-session-store]]
            [clojure.tools.logging :refer [info debug spy]]))

(defn prod-system [config-options]
  (info "Running the production system")
  (let [{:keys [port event-store-config internal-api-port]} config-options]
    (component/system-map
     :config-options config-options
     :internal-api-handler (component/using
                            (internal-api-component)
                            [:event-store])
     :internal-api-jetty (component/using
                          (jetty-component internal-api-port)
                          [:internal-api-handler])
     :session-store {:session-store (redis-session-store {:some :config})}
     :ring-handler (component/using
                    (ring-handler-component)
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

(def prod-config {:port 3000
                  :internal-api-port 3001
                  :event-store-config {:uri (or (env :event-store-uri) "http://127.0.0.1:2113")
                                       :user (or (env :event-store-user) "admin")
                                       :password (or (env :event-store-password) "changeit")}})
