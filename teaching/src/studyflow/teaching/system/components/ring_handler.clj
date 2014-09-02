(ns studyflow.teaching.system.components.ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.teaching.web :as web]
            [clojure.tools.logging :as log]
            [studyflow.teaching.web.authentication :refer [wrap-authentication]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn site-config [config]
  (-> config
      (assoc-in [:static :resources] "teaching/public")))

(defn wrap-redirect-urls
  [f urls]
  (fn [r]
    (f (assoc r :redirect-urls urls))))

(defrecord RingHandlerComponent [read-model redirect-urls session-store]
  Lifecycle
  (start [component]
    (log/info "Starting teaching ring handler")
    (assoc component :handler
           (-> web/app
               (wrap-authentication session-store)
               (wrap-redirect-urls redirect-urls)
               (web/wrap-read-model (:read-model read-model))
               (wrap-defaults (site-config site-defaults)))))
  (stop [component]
    (log/info "Stopping teaching ring handler")
    component))

(defn ring-handler-component [redirect-urls]
  (map->RingHandlerComponent {:redirect-urls redirect-urls}))
