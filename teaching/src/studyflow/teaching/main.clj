(ns studyflow.teaching.main
  (:require [clojure.tools.logging :as log]
            [studyflow.teaching.system :as system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main [jetty-port event-store-uri login-url teaching-url session-store-url]
  (log/info "Studyflow teaching app")
  (let [s (-> (system/prod-system {:port (Long/parseLong jetty-port)
                                   :secure-site-defaults? true
                                   :event-store-config event-store-uri
                                   :sesion-store-url session-store-url
                                   :redirect-urls {:login login-url
                                                   :teaching teaching-url}})
              component/start)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (component/stop s)
                                 (log/info "Stopping is done, bye"))))))
