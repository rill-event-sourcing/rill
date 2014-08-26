(ns studyflow.components.psql-event-store
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store.psql.pool :as pool]
            [rill.repository :refer [wrap-caching-repository]]
            [studyflow.migrations.active-migrations :refer [wrap-active-migrations]]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord PsqlEventStoreComponent [spec store connection]
  Lifecycle
  (start [component]
    (info "Starting psql event-store")
    (let [connection (pool/open spec)]
      (-> component
          (assoc :connection connection)
          (assoc :store (-> connection
                            psql-event-store
                            wrap-active-migrations
                            wrap-caching-repository)))))
  (stop [component]
    (info "Stopping in-psql event-store")
    (pool/close connection)
    (dissoc component :store :connection)))

(defn psql-event-store-component [spec]
  (map->PsqlEventStoreComponent {:spec spec}))
