(ns login-dev-system
  (:require [studyflow.login.system :as system]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [com.stuartsierra.component :as component]))

(defn make-system [dev-options]
  (merge (system/make-system dev-options)
         {:event-store (component/using
                        (memory-event-store-component)
                        [])}))
