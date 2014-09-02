(ns studyflow.teaching.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.web :as web]
            [clojure.tools.logging :as log]
            [studyflow.teaching.web.authentication :refer [wrap-authentication]]
            [studyflow.web.authentication :refer [wrap-check-cookie wrap-redirect-urls wrap-cookie-domain]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn site-config [config]
  (-> config
      (assoc-in [:static :resources] "teaching/public")))

(defrecord RingHandlerComponent [read-model redirect-urls session-store cookie-domain]
  Lifecycle
  (start [component]
    (log/info "Starting teaching ring handler")
    (assoc component :handler
           (-> web/app
               (wrap-authentication session-store)
               (wrap-check-cookie)
               (wrap-redirect-urls redirect-urls)
               (wrap-cookie-domain cookie-domain)
               (web/wrap-read-model (:read-model read-model))
               (wrap-defaults (site-config site-defaults)))))
  (stop [component]
    (log/info "Stopping teaching ring handler")
    component))

(defn ring-handler-component [redirect-urls cookie-domain]
  (map->RingHandlerComponent {:redirect-urls redirect-urls :cookie-domain cookie-domain}))
