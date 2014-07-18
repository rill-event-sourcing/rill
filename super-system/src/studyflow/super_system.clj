(ns studyflow.super-system
  (:require [com.stuartsierra.component :refer [system-map] :as component]
            [studyflow.login.system :as login]
            [studyflow.system :as learning]
            [clojure.tools.logging :as log]
            [learning-dev-system]))

(defn make-system [_]
  (let [learning (learning-dev-system/dev-system (assoc learning-dev-system/dev-config
                                                   :jetty-port 3000))
        event-store (:event-store learning)
        login (-> (login/make-system {:jetty-port 4000})
                  (assoc :event-store event-store))]
    (system-map :learning-system learning
                :login-system login)))
