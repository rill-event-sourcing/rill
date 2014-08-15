(ns studyflow.school-administration.eduroute-listener
  (:require [clojure.core.async :refer [go close! <! <!!]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.uuid :refer [new-id uuid]]
            [studyflow.school-administration.student :as student]))

(defn with-claim [event-store claim main revert-claim]
  (let [[claim-status :as claim-result] (try-command event-store claim)]
    (if (= :ok claim-status)
      (let [[main-status :as main-result] (try-command event-store main)]
        (when-not (= :ok main-status)
          (try-command event-store revert-claim))
        main-result)
      claim-result)))

(defn handle-edu-route-event
  [event-store {:keys [edu-route-id full-name]}]
  (let [student-id (new-id)]
    (with-claim event-store
      (student/claim-edu-route-id! edu-route-id student-id)
      (student/create-from-edu-route-credentials! student-id edu-route-id full-name)
      (student/release-edu-route-id! edu-route-id student-id))))

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
