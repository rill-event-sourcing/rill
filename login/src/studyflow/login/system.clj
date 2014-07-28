(ns studyflow.login.system
  (:require [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :as component]
            [studyflow.login.credentials :as credentials]
            [studyflow.login.main :as main]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.login.edu-route-mock-service :refer [edu-route-mock-service]]
            [studyflow.login.edu-route-production-service :refer [edu-route-production-service]])
  (:import [org.apache.log4j Logger]))

(defrecord CredentialsComponent [event-channel]
  component/Lifecycle

  (start [component]
    (log/info "Starting credentials")
    (let [db (atom {:by-email {"editor@studyflow.nl"
                               {:user-id "editor-id" :user-role "editor" :encrypted-password (bcrypt/encrypt "editor")}}
                    :by-edu-route-id {"12345"
                                      {:user-role "student" :user-id "some student id"}}
})]
      (credentials/listen! (:channel event-channel) db)
      (assoc component
        :credentials-db db
        :authenticate-by-email-and-password-fn #(credentials/authenticate-by-email-and-password @db %1 %2)
        :authenticate-by-edu-route-id-fn #(credentials/authenticate-by-edu-route-id @db %1))))

  (stop [component]
    (log/info "Stopping credentials")
    (dissoc component :credentials-db)))

(defn credentials-component []
  (map->CredentialsComponent {}))

(defrecord RingHandlerComponent [credentials session-store event-store edu-route-service]
  component/Lifecycle

  (start [component]
    (log/info "Starting handler")
    (assoc component :handler
           (fn [req]
             (main/app (assoc req
                         :authenticate-by-email-and-password (:authenticate-by-email-and-password-fn credentials)
                         :edu-route-service edu-route-service
                         :authenticate-by-edu-route-id (:authenticate-by-edu-route-id-fn credentials)
                         :event-store (:store event-store)
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
   :ring-handler (component/using (ring-handler-component) [:credentials :session-store :event-store :edu-route-service])
   :edu-route-service (edu-route-production-service "DDF9nh3w45s$Wo1w" "studyflow" "qW3#f65S") ;; TODO: get implementation from config
   :session-store (redis-session-store {:some :config})
   :credentials (component/using (credentials-component) [:event-channel])
   :event-channel (component/using (event-channel-component) [:event-store])
   :event-store (component/using (memory-event-store-component) [])))
