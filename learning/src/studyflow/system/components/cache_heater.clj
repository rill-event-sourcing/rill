(ns studyflow.system.components.cache-heater
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.components.event-channel :refer [channel]]
            [clojure.tools.logging :refer [info debug spy]]
            [clojure.core.async :refer [thread <!! close!]]
            [studyflow.learning.course.events :as course]
            [rill.message :as message]
            [rill.repository :refer [retrieve-aggregate]]
            [rill.event-channel :as event-channel]
            [studyflow.web.app-status :refer [healthy-response starting-up-response]]))

(defrecord CacheHeater [event-store event-channel num]
  Lifecycle
  (start [component]
    (info "Starting cache heater")
    (let [ch (channel event-channel num)
          done? (atom false)]
      (thread
        (loop []
          (when-let [e (<!! ch)]
            (when (= (message/type e) ::course/Published)
              (retrieve-aggregate (:store event-store) (:course-id e)))
            (when-not (= (message/type e) ::event-channel/CaughtUp)
              (recur))))
        (close! ch)
        (info "Caught up with all events. Cache warmed up")
        (reset! done? true))
      (assoc component :done? done?)))
  (stop [component]
    component))

(defn cache-heater [num]
  (map->CacheHeater {:num num}))

(defn wrap-cache-status
  [handler cache-heater]
  (fn [{:keys [uri] :as request}]
    (if @(:done? cache-heater)
      (if (= uri "/health-check")
        healthy-response
        (handler request))
      starting-up-response)))

