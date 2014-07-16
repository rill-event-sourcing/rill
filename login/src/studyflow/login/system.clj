(ns studyflow.login.system
  (:require [clojure.tools.logging :as log]
            [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :as component]
            [studyflow.login.credentials :as credentials]
            [studyflow.login.main :as main]
            [studyflow.components.jetty :refer [jetty-component]]
            [studyflow.components.event-channel :refer [event-channel-component]]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]])
  (:import [org.apache.log4j Logger]))

(defrecord CredentialsComponent [event-channel-component]
  component/Lifecycle

  (start [component]
    (log/info "Starting credentials")
    (let [db (atom {:by-email {"editor@studyflow.nl"
                               {:uuid "editor-id" :role "editor" :encrypted-password (bcrypt/encrypt "editor")}}
                    :by-edu-route-id {"12345"
                                      {:role "student" :uuid "some student id"}}
})]
      (credentials/listen! (:channel event-channel-component) db)
      (assoc component
        :credentials-db db
        :authenticate-by-email-and-password-fn #(credentials/authenticate-by-email-and-password @db %1 %2)
        :authenticate-by-edu-route-id-fn #(credentials/authenticate-by-edu-route-id @db %1))))

  (stop [component]
    (log/info "Stopping credentials")
    (dissoc component :credentials-db)))

(defn credentials-component []
  (map->CredentialsComponent {}))

(defrecord RingHandlerComponent [credentials-component event-store]
  component/Lifecycle

  (start [component]
    (log/info "Starting handler")
    (assoc component :handler
           (fn [req]
             (prn [(:store event-store)])
             (main/app (assoc req
                         :authenticate-by-email-and-password (:authenticate-by-email-and-password-fn credentials-component)
                         :authenticate-by-edu-route-id (:authenticate-by-edu-route-id-fn credentials-component)
                         :event-store (:store event-store))))))

  (stop [component]
    (log/info "Stopping handler")
    (dissoc component :handler)))

(defn ring-handler-component []
  (map->RingHandlerComponent {}))

(defonce system nil)

(defn init
  ([config]
     (let [sm (component/system-map
               :jetty (component/using (jetty-component (:jetty-port config)) [:ring-handler])
               :ring-handler (component/using (ring-handler-component) [:credentials-component :event-store])
               :credentials-component (component/using (credentials-component) [:event-channel-component])
               :event-channel-component (component/using (event-channel-component) [:event-store])
               :event-store (component/using (memory-event-store-component) []))]
       (alter-var-root (var system) (constantly sm))))
  ([]
     (init {:jetty-port 4000})))

(defn start []
  (alter-var-root (var system) component/start))

(defn stop []
  (alter-var-root (var system) component/stop))

(defn reset []
  (stop)
  (init)
  (start))

;; from cascalog playground for swank/slime
(defn bootstrap-emacs []
  (let [logger (Logger/getRootLogger)]
    (doto (. logger (getAppender "stdout"))
      (.setWriter *out*))
    (alter-var-root #'clojure.test/*test-out* (constantly *out*))
    (log/info "Logging to repl")))
