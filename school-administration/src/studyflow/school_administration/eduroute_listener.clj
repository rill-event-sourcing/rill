(ns studyflow.school-administration.eduroute-listener
  (:require [clojure.core.async :refer [go close! <! <!!]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [studyflow.credentials.edu-route-id-ownership :as edu-route-id-ownership]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.uuid :refer [new-id uuid]]
            [studyflow.command-tools :refer [with-claim]]
            [studyflow.school-administration.student :as student]))

(defn handle-edu-route-event
  [event-store {:keys [edu-route-id full-name]}]
  (let [student-id (new-id)]
    (with-claim event-store
      (edu-route-id-ownership/claim! edu-route-id student-id)
      (student/create-from-edu-route-credentials! student-id edu-route-id full-name)
      (edu-route-id-ownership/release! edu-route-id student-id))))

(defn eduroute-listener [event-store event-channel]
  (go (loop []
        (when-let [event (<! event-channel)]
          (try (when (= (message/type event) :studyflow.login.edu-route-student.events/Registered)
                 (handle-edu-route-event event-store event))
               (catch Throwable e
                 (log/error e "Error in eduroute-listener")))
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
