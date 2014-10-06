(ns studyflow.teaching.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.web :as web]
            [clojure.tools.logging :as log]
            [studyflow.teaching.web.authentication :refer [wrap-authentication]]
            [studyflow.web.authentication :refer [wrap-redirect-urls]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn site-config [config session-store cookie-domain]
  (-> config
      (assoc-in [:static :resources] "teaching/public")
      (assoc-in [:session :store] session-store)
      (assoc-in [:session :cookie-attrs :domain] cookie-domain)))

(defrecord RingHandlerComponent [read-model redirect-urls session-store cookie-domain]
  Lifecycle
  (start [component]
    (log/info "Starting teaching ring handler")
    (assoc component :handler
           (-> web/app
               wrap-authentication
               (wrap-redirect-urls redirect-urls)
               (web/wrap-read-model (:read-model read-model))
               (wrap-defaults (site-config site-defaults session-store cookie-domain)))))
  (stop [component]
    (log/info "Stopping teaching ring handler")
    component))

(defn ring-handler-component [redirect-urls cookie-domain]
  (map->RingHandlerComponent {:redirect-urls redirect-urls :cookie-domain cookie-domain}))
