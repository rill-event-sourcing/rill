(ns studyflow.system
  (:require [clojure.tools.logging :refer [info debug spy]]
            [environ.core :refer [env]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-store.atom-store :as atom-store]
            [studyflow.learning.read-model :refer [empty-model]]
            [studyflow.learning.read-model.event-listener :refer [listen!]]
            [studyflow.web :as web]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [close! <!!]]
            [clj-http.client :as client]))

(defrecord JettyComponent [port ring-handler]
  component/Lifecycle
  (start [component]
    (info "Starting jetty on port: " port)
    (assoc component :jetty (jetty/run-jetty (:handler ring-handler) {:port port
                                                                      :join? false})))
  (stop [component]
    (info "Stopping jetty")
    (when-let [jetty (:jetty component)]
      (when-not (.isStopped jetty)
        (.stop jetty)))
    component))

(defn jetty-component [port]
  (map->JettyComponent {:port port}))

(defrecord RingHandlerComponent [event-store read-model]
  component/Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler (web/make-request-handler (:store event-store) (:read-model read-model))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn ring-handler-component []
  (map->RingHandlerComponent {}))

(defrecord EventStoreComponent [config]
  component/Lifecycle
  (start [component]
    (info "Starting event-store")
    (when (not= 200 (:status (try (client/get (:uri config))
                                  (catch Exception e nil))))
      (throw (Exception. (str "Can't connect to EventStore at " (:uri config)))))
    (assoc component :store (atom-store/atom-event-store (:uri config) (select-keys config [:user :password]))))
  (stop [component]
    (info "Stopping event-store")
    component))

(defn event-store-component [config]
  (map->EventStoreComponent {:config config}))

(defrecord ReadModelComponent [event-channel]
  component/Lifecycle
  (start [component]
    (info "Starting read-model")
    (let [read-model (atom empty-model)]
      (assoc component
        :read-model read-model
        :event-listener (listen! read-model (:channel event-channel)))))
  (stop [component]
    (info "Stopping read-model")
    ;; this has a dependency on the event-channel, which will start
    ;; before as and therefore close after us
    ;; this will only finish when the channel is closed
    (when (:read-model component)
      (close! (:channel event-channel))
      (<!! (:event-listener component)))
    component))

(defn read-model-component []
  (map->ReadModelComponent {}))

(defrecord EventChannelComponent [event-store]
  component/Lifecycle
  (start [component]
    (info "Starting event-channel")
    (assoc component
      :channel (event-channel (:store event-store) "%24all" -1 0)))
  (stop [component]
    (info "Stopping event-channel")
    (when-let [c (:channel component)]
      (close! c))
    component))

(defn event-channel-component []
  (map->EventChannelComponent {}))

(defn prod-system [config-options]
  (info "Running the production system")
  (let [{:keys [port event-store-config]} config-options]
    (component/system-map
     :config-options config-options
     :ring-handler (component/using
                    (ring-handler-component)
                    [:event-store :read-model])
     :jetty (component/using
             (jetty-component port)
             [:ring-handler])
     :event-channel (component/using
                     (event-channel-component)
                     [:event-store])
     :event-store (component/using
                   (event-store-component event-store-config)
                   [])
     :read-model (component/using
                  (read-model-component)
                  [:event-store :event-channel]))))

(def prod-config {:port 3000
                  :event-store-config {:uri (or (env :event-store-uri) "http://127.0.0.1:2113")
                                       :user (or (env :event-store-user) "admin")
                                       :password (or (env :event-store-password) "changeit")}})
