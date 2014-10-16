(ns studyflow.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.learning.web :as web]
            [studyflow.system.components.cache-heater :refer [wrap-cache-status]]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord RingHandlerComponent [event-store read-model redirect-urls session-store cookie-domain cache-heater]
  Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler (-> (web/make-request-handler (:store event-store)
                                                            (:read-model read-model)
                                                            redirect-urls
                                                            cookie-domain
                                                            session-store)
                                  (wrap-cache-status cache-heater))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn ring-handler-component [redirect-urls cookie-domain]
  (map->RingHandlerComponent {:redirect-urls redirect-urls :cookie-domain cookie-domain}))
