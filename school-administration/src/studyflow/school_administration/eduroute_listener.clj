(ns studyflow.school-administration.eduroute-listener
  (:require [clojure.core.async :refer [go close! <! <!!]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.uuid :refer [new-id uuid]]
            [studyflow.school-administration.student :as student]))

(defn eduroute-listener [event-store event-channel]
  (go (loop []
        (when-let [event (<! event-channel)]
          (when (= (message/type event) :studyflow.login.edu-route-student.events/Registered)
            (try-command event-store (student/create-from-edu-route-credentials! (new-id) (:edu-route-id event) (:full-name event))))
          (recur)))))

(defrecord EdurouteListenerComponent [event-store event-channel]
  component/Lifecycle
  (start [component]
    (log/info "Starting eduroute listener")
    (assoc component :listener (eduroute-listener (:store event-store) (:channel event-channel))))
  (stop [component]
    (log/info "Stopping eduroute listener")
    (when (:listener component)
      (close! (:channel event-channel))
      (<!! (:listener component)))
    (dissoc component :listener)))

(defn eduroute-listener-component []
  (map->EdurouteListenerComponent {}))
