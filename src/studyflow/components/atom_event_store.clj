(ns studyflow.components.atom-event-store
  (:require [clj-http.client :as client]
            [clojure.tools.logging :refer [info]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [rill.event-store.atom-store :as atom-store]))

(defrecord AtomEventStoreComponent [config]
  Lifecycle
  (start [component]
    (info "Starting event-store")
    (when (not= 200 (:status (try (client/get (:uri config))
                                  (catch Exception e nil))))
      (throw (Exception. (str "Can't connect to EventStore at " (:uri config)))))
    (assoc component :store (atom-store/atom-event-store (:uri config) (select-keys config [:user :password]))))
  (stop [component]
    (info "Stopping event-store")
    component))

(defn atom-event-store-component [config]
  (map->AtomEventStoreComponent {:config config}))
