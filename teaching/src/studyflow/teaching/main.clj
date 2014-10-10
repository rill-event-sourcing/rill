(ns studyflow.teaching.main
  (:require [clojure.tools.logging :as log]
            [studyflow.teaching.system :as system]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:gen-class))

(defn -main []
  (let [{:keys [jetty-port eventstore-url login-url teaching-url sessionstore-url cookie-domain]} env]
    (assert (every? seq [jetty-port eventstore-url login-url teaching-url sessionstore-url cookie-domain]))
    (log/info "Studyflow teaching app")
    (let [s (-> (system/prod-system {:port (Long/parseLong jetty-port)
                                     :event-store-config eventstore-url
                                     :session-store-url sessionstore-url
                                     :redirect-urls {:login login-url
                                                     :teaching teaching-url}
                                     :cookie-domain cookie-domain})
                component/start)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (component/stop s)
                                   (log/info "Stopping is done, bye")))))))
