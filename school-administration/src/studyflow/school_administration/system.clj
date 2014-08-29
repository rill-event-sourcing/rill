(ns studyflow.school-administration.system
  (:require [com.stuartsierra.component :as component]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.components.jetty :refer [jetty-component]]
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
     :ring-handler (component/using
                    (ring-handler-component secure-site-defaults?)
                    [:event-store :read-model])
     :jetty (component/using
             (jetty-component port)
             [:ring-handler])
     :event-channel (component/using
                     (event-channel-component 2)
                     [:event-store])
     :event-store (component/using
                   (psql-event-store-component event-store-config)
                   [])
     :read-model (component/using
                  (read-model-component 0)
                  [:event-store :event-channel])
     :eduroute-listener (component/using
                         (eduroute-listener-component 1)
                         [:event-channel :event-store])
     :uncaught-exception-handler (component/using
                                  (uncaught-exception-handler-component)
                                  []))))
