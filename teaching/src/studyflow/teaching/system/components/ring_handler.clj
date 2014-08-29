(ns studyflow.teaching.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.web :as web]
            [clojure.tools.logging :as log]
            [studyflow.teaching.web.authentication :refer [wrap-authentication]]
            [ring.middleware.defaults :refer [secure-site-defaults site-defaults wrap-defaults]]))

(defn site-config [config]
  (-> config
      (assoc-in [:static :resources] "teaching/public")))

(defn wrap-redirect-urls
  [f urls]
  (fn [r]
    (f (assoc r :redirect-urls urls))))

(defrecord RingHandlerComponent [secure-site-defaults? read-model redirect-urls session-store]
  Lifecycle
  (start [component]
    (log/info "Starting teaching ring handler")
    (assoc component :handler
           (-> web/app
               (wrap-authentication session-store)
               (wrap-redirect-urls redirect-urls)
               (web/wrap-read-model (:read-model read-model))
               (wrap-defaults (site-config (if secure-site-defaults?
                                             secure-site-defaults
                                             site-defaults))))))
  (stop [component]
    (log/info "Stopping teaching ring handler")
    component))

(defn ring-handler-component [secure-site-defaults? redirect-urls]
  (map->RingHandlerComponent {:secure-site-defaults? secure-site-defaults?
                              :redirect-urls redirect-urls}))
