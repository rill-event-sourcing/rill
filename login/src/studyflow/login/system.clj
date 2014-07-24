(ns studyflow.login.system
  (:require [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :as component]
            [studyflow.login.credentials :as credentials]
            [studyflow.login.main :as main]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]))

(defrecord CredentialsComponent [event-channel]
  component/Lifecycle

  (start [component]
    (log/info "Starting credentials")
    (let [store (atom {"editor@studyflow.nl" {:user-id "editor-id" :user-role "editor" :encrypted-password (bcrypt/encrypt "editor")}})]
      (credentials/listen! (:channel event-channel) store)
      (assoc component
        :credentials-store store
        :authenticate-fn #(credentials/authenticate @store %1 %2))))

  (stop [component]
    (log/info "Stopping credentials")
    (dissoc component :credentials-store)))

(defn credentials-component []
  (map->CredentialsComponent {}))

(defrecord RingHandlerComponent [credentials session-store]
  component/Lifecycle

  (start [component]
    (log/info "Starting handler")
    (assoc component :handler
           (fn [req]
             (main/app (assoc req
                         :authenticate (:authenticate-fn credentials)
                         :session-store session-store)))))

  (stop [component]
    (log/info "Stopping handler")
    (dissoc component :handler)))

(defn ring-handler-component []
  (map->RingHandlerComponent {}))

(defn make-system
  [config]
  (component/system-map
   :jetty (component/using (jetty-component (:jetty-port config)) [:ring-handler])
   :ring-handler (component/using (ring-handler-component) [:credentials :session-store])
   :session-store (redis-session-store)
   :credentials (component/using (credentials-component) [:event-channel])
   :event-channel (component/using (event-channel-component) [:event-store])
   :event-store (component/using (memory-event-store-component) [])))
