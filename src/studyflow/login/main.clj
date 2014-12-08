(ns studyflow.login.main
  (:gen-class)
  (:require [studyflow.login.system :as system]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]))

(defn -main []
  (let [{:keys [jetty-port publishing-url learning-url teaching-url eventstore-url sessionstore-url session-max-age cookie-domain]} env]
    (assert (every? seq [jetty-port publishing-url learning-url teaching-url eventstore-url sessionstore-url session-max-age cookie-domain]))
    (let [system (system/make-system {:jetty-port (Long/parseLong jetty-port)
                                      :default-redirect-paths {"editor" publishing-url
                                                               "student" learning-url
                                                               "teacher" teaching-url}
                                      :event-store-config eventstore-url
                                      :session-store-url sessionstore-url
                                      :cookie-domain cookie-domain})]
      (component/start system))))
