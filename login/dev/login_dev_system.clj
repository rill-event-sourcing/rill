(ns login-dev-system
  (:require [studyflow.login.system :as system]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [studyflow.components.simple-session-store :refer [simple-session-store]]
            [com.stuartsierra.component :as component]))

(def dev-config  {:session-store-config {:uri "redis://localhost:7890"}
                  :session-max-age (* 8 60 60)
                  :cookie-domain nil})

(defn make-system [dev-options]
  (merge (system/make-system dev-options)
         {:event-store (component/using
                        (memory-event-store-component)
                        [])
          :session-store (simple-session-store)}))
