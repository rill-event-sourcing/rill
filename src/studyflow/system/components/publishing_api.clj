(ns studyflow.system.components.publishing-api
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.learning.web.publishing-api :as publishing-api]))

(defrecord PublishingApiComponent [event-store]
  Lifecycle
  (start [component]
    (log/info "Starting publishing-api handler")
    (assoc component :handler (publishing-api/make-request-handler (:store event-store))))
  (stop [component]
    (log/info "Stopping publishing-api handler")
    component))

(defn publishing-api-component []
  (map->PublishingApiComponent {}))
