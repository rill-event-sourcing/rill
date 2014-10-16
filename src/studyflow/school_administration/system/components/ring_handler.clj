(ns studyflow.school-administration.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.school-administration.web :as web]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord RingHandlerComponent [secure-site-defaults? event-store read-model]
  Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler (web/make-request-handler secure-site-defaults? (:store event-store) (:read-model read-model))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn ring-handler-component [secure-site-defaults?]
  (map->RingHandlerComponent {:secure-site-defaults? secure-site-defaults?}))
