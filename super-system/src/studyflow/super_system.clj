(ns studyflow.super-system
  (:require [com.stuartsierra.component :refer [system-map] :as component]
            [studyflow.login.system :as login]
            [studyflow.system :as learning]
            [clojure.tools.logging :as log]))

(defn make-system [_]
  (system-map :login-system (login/make-system {:jetty-port 4000})
              #_:learning-system #_(learning/prod-system learning/prod-config)))


