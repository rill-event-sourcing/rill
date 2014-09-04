(ns studyflow.components.uncaught-exception-handler
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :refer [Lifecycle]]
            [environ.core :refer [env]]
            [ring.util.response :refer [resource-response]])
  (:import (com.mindscapehq.raygun4java.core RaygunClient)))

(defn report
  [throwable & [tags meta]]
  (log/error throwable "Uncaught Exception:" meta)
  (if-let [raygun-api-key (env :raygun-api-key)]
    (let [client (RaygunClient. raygun-api-key)]
      (.Send client throwable tags meta))))

(def uncaught-exception-handler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ throwable]
      (report throwable ["uncaught-exception"]))))

(defn- replace-default-uncaught-exception-handler [handler]
  (let [old (Thread/getDefaultUncaughtExceptionHandler)]
    (Thread/setDefaultUncaughtExceptionHandler handler)
    old))

(defrecord UncaughtExceptionHandlerComponent []
  Lifecycle
  (start [component]
    (log/info "Starting uncaught exception handler")
    (let [old-handler (replace-default-uncaught-exception-handler uncaught-exception-handler)]
      (assoc component :old-uncaught-exception-handler old-handler)))
  (stop [component]
    (log/info "Stopping uncaught exception handler")
    (replace-default-uncaught-exception-handler (:old-uncaught-exception-handler component))
    (dissoc component :old-uncaught-exception-handler)))

(defn uncaught-exception-handler-component []
  (map->UncaughtExceptionHandlerComponent {}))

(defn wrap-uncaught-exception [app]
  (fn [req]
    (try
      (app req)
      (catch Throwable e
        (report e ["wrap-uncaught-exception"] req)
        (-> (resource-response "public/500.html")
            (assoc :status 500)
            (assoc-in [:headers "Content-Type"] "text/html"))))))
