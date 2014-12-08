(ns studyflow.main
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.system :as system]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:gen-class))

(defn -main []
  (let [{:keys [jetty-port learning-url teaching-url login-url eventstore-url sessionstore-url  cookie-domain]} env]
    (assert (every? seq [jetty-port learning-url teaching-url login-url eventstore-url sessionstore-url cookie-domain]))
    (log/info "Main Studyflow learning app")
    (let [s (-> (system/prod-system {:port (Long/parseLong jetty-port)
                                     :internal-api-port 3001
                                     :event-store-config eventstore-url
                                     :session-store-url sessionstore-url
                                     :cookie-domain cookie-domain
                                     :redirect-urls {:login login-url
                                                     :learning learning-url
                                                     :teacher teaching-url}})
                component/start)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (component/stop s)
                                   (log/info "Stopping is done, bye")))))))
