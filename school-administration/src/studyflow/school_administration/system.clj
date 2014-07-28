(ns studyflow.school-administration.system
  (:require [com.stuartsierra.component :as component]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.school-administration.system.components.read-model :refer [read-model-component]]
            [studyflow.school-administration.system.components.ring-handler :refer [ring-handler-component]]
            [clojure.tools.logging :as log]))

(defn prod-system [config-options]
  (log/info "Running the production system")
  (let [{:keys [port event-store-config]} config-options]
    (component/system-map
     :config-options config-options
     :ring-handler (component/using
                    (ring-handler-component)
                    [:event-store :read-model])
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

