(ns studyflow.login.system
  (:require [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :as component :refer [using]]
            [studyflow.login.credentials :as credentials]
            [studyflow.login.main :as main]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.simple-session-store :refer [simple-session-store]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.components.event-channel :refer [event-channel-component channel]]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [studyflow.components.atom-event-store :refer [atom-event-store-component]]
            [studyflow.web.app-status :refer [app-status-component]]
            [studyflow.components.uncaught-exception-handler :refer [uncaught-exception-handler-component]]
            [studyflow.login.edu-route-mock-service :refer [edu-route-mock-service]]
            [studyflow.login.edu-route-production-service :refer [edu-route-production-service]]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]])
  (:import [org.apache.log4j Logger]))

(defrecord CredentialsComponent [event-channel num]
  component/Lifecycle

  (start [component]
    (log/info "Starting credentials")
    (let [db (atom {:by-email {"editor@studyflow.nl"
                               {:user-id "editor-id" :user-role "editor" :encrypted-password (bcrypt/encrypt "editor")}}
                    :by-edu-route-id {"12345"
                                      {:user-role "student" :user-id "some student id"}}})]
      (credentials/listen! (channel event-channel num) db)
      (assoc component
        :credentials-db db
        :authenticate-by-email-and-password-fn #(credentials/authenticate-by-email-and-password @db %1 %2)
        :authenticate-by-edu-route-id-fn #(credentials/authenticate-by-edu-route-id @db %1))))

  (stop [component]
    (log/info "Stopping credentials")
    (dissoc component :credentials-db)))

(defn credentials-component [channel-number]
  (map->CredentialsComponent {:num channel-number}))

(defrecord RingHandlerComponent [credentials session-store event-store edu-route-service default-redirect-paths session-max-age cookie-domain]
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
                         :default-redirect-paths default-redirect-paths
                         :credentials @(:credentials-db credentials)
                         :session-max-age session-max-age
                         :cookie-domain cookie-domain
                         :session-store session-store)))))

  (stop [component]
    (log/info "Stopping handler")
    (dissoc component :handler)))

(defn ring-handler-component [session-max-age default-redirect-paths cookie-domain]
  (map->RingHandlerComponent {:session-max-age session-max-age
                              :default-redirect-paths default-redirect-paths
                              :cookie-domain cookie-domain}))

(defn make-system
  [{:keys [jetty-port default-redirect-paths event-store-config session-store-config session-max-age cookie-domain]}]
  (component/system-map
   :jetty (-> (jetty-component (or jetty-port 4000))
              (using [:ring-handler :app-status-component]))
   :ring-handler (-> (ring-handler-component (or session-max-age (* 8 60 60))
                                                          (merge {"editor" "http://localhost:2000"
                                                                  "student" "http://localhost:3000"
                                                                  "teacher" "http://example.com"}
                                                                 default-redirect-paths)
                                                          cookie-domain)
                     (using [:credentials :session-store :event-store :edu-route-service]))
   :edu-route-service (edu-route-production-service "DDF9nh3w45s$Wo1w" "studyflow" "qW3#f65S") ;; TODO: get implementation from config
   :session-store (redis-session-store session-store-config)
   :app-status-component (-> (app-status-component 1)
                             (using [:event-channel]))
   :credentials (-> (credentials-component 0)
                    (using [:event-channel]))
   :event-channel (-> (event-channel-component 2)
                      (using [:event-store]))
   :event-store (psql-event-store-component event-store-config)
   :uncaught-exception-handler (uncaught-exception-handler-component)))
