(ns studyflow.web.app-status
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.loop-tools :refer [while-let]]
            [studyflow.components.event-channel :refer [channel]]
            [rill.message :as message]
            [clojure.core.async :refer [close! <!! thread]]))

(defn listen!
  "listen on event-channel"
  [event-channel status-atom]
  (thread
    (while-let [e (<!! event-channel)]
               (when (= (message/type e) :rill.event-channel/CaughtUp)
                 (reset! status-atom :caught-up)))))

(defrecord WebStatusComponent [event-channel num status-atom]
  Lifecycle
  (start [component]
    (assoc component
      :event-listener (listen! (channel event-channel num) status-atom)))
  (stop [component]
    (when-let [c (channel event-channel num)]
      (close! c))
    (dissoc component :event-listener)))

(defn app-status-component [channel-number]
  (map->WebStatusComponent {:num channel-number :status-atom (atom nil)}))

(defn caught-up?
  [component]
  (= @(:status-atom component) :caught-up))

(def starting-up-response
  {:status 503
   :body "{\"status\":\"catching up\"}"
   :headers {"Content-Type" "text/plain"}})

(def healthy-response
  {:status 200
   :body "{\"status\":\"up\"}"
   :headers {"Content-Type" "text/plain"}})

(defn wrap-app-status
  [handler app-status-component]
  (fn [{:keys [uri] :as request}]
    (if (caught-up? app-status-component)
      (if (= uri "/health-check")
        healthy-response
        (handler request))
      starting-up-response)))
