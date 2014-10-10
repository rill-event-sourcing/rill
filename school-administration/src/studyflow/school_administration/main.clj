(ns studyflow.school-administration.main
  (:require [clojure.tools.logging :as log]
            [studyflow.school-administration.system :as system]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:gen-class))

(defn -main []
  (let [{:keys [jetty-port eventstore-url]} env]
    (assert (every? seq [jetty-port eventstore-url]))
    (log/info "Studyflow school administration app")
    (let [s (-> (system/prod-system {:port (Long/parseLong jetty-port)
                                     :secure-site-defaults? true
                                     :event-store-config eventstore-url})
                component/start)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (component/stop s)
                                   (log/info "Stopping is done, bye")))))))
