(ns studyflow.teaching.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.web :as web]
            [clojure.tools.logging :as log]
            [ring.middleware.defaults :refer [secure-site-defaults site-defaults wrap-defaults]]))

(defn site-config [config]
  (-> config
      (assoc-in [:static :resources] "teaching/public")))

(defrecord RingHandlerComponent [secure-site-defaults? read-model]
  Lifecycle
  (start [component]
    (log/info "Starting teaching ring handler")
    (assoc component :handler
           (-> web/app
               (web/wrap-read-model (:read-model read-model))
               (wrap-defaults (site-config (if secure-site-defaults?
                                             secure-site-defaults
                                             site-defaults))))))
  (stop [component]
    (log/info "Stopping teaching ring handler")
    component))

(defn ring-handler-component [secure-site-defaults?]
  (map->RingHandlerComponent {:secure-site-defaults? secure-site-defaults?}))
