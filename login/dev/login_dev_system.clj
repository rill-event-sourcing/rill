(ns login-dev-system
  (:require [studyflow.login.system :as system]
            [studyflow.components.simple-session-store :refer [simple-session-store]]
            [com.stuartsierra.component :as component]))

(defn make-system [dev-options]
  (merge (system/make-system dev-options)
         {:session-store (simple-session-store)}))
