(ns learning-dev-system
  (:require [studyflow.system :as sys]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [ring.middleware.session.memory :refer [memory-store]]
            [studyflow.system.components.dev-ring-handler :refer [dev-ring-handler-component]]
            [studyflow.system.components.fixtures-loading :refer [fixtures-loading-component]]
            [com.stuartsierra.component :as component]))

(def dev-config {:port 3000
                 :internal-api-port 3001
                 :redirect-urls {:login "http://localhost:4000"
                                 :learning "http://localhost:3000"}})

(defn dev-system [dev-options]
  (merge (sys/prod-system dev-options)
         {:ring-handler (component/using
                         (dev-ring-handler-component (:redirect-urls dev-options)
                                                     nil)
                         [:event-store :read-model :session-store])
          :session-store (memory-store)
          :event-store (component/using
                        (memory-event-store-component)
                        [])}
         (when-not (:no-fixtures dev-options)
           {:fixtures-loading (component/using
                               (fixtures-loading-component)
                               {:ring-handler :publishing-api-handler})})))
