(ns studyflow.main
  (:require [clojure.tools.logging :as log]
            [studyflow.system :as system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main [jetty-port learning-url login-url event-store-uri session-store-uri & [cookie-domain]]
  (log/info "Main Studyflow learning app")
  (let [s (-> (system/prod-system {:port (Long/parseLong jetty-port)
                                   :internal-api-port 3001
                                   :event-store-config event-store-uri
                                   :session-store-config {:uri session-store-uri}
                                   :coolie-domain cookie-domain
                                   :redirect-urls {:login login-url
                                                   :learning learning-url}})
              component/start)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (component/stop s)
                                 (log/info "Stopping is done, bye"))))))
