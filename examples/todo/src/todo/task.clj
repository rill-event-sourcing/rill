(ns todo.task
  (:require [clojure.core.async :refer [go-loop <!]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [todo.aggregates.task :as task-aggregate]
            [todo.commands.task :as task-command])
  (:import [java.util UUID]))

(defonce store (atom nil))

(defn create! [description]
  (try-command @store
               (task-command/create! (UUID/randomUUID) description)))

(defn update-description! [uuid description]
  (try-command @store
               (task-command/update-description! uuid description)))

(defn delete! [uuid]
  (try-command @store
               (task-command/delete! uuid)))

(defonce by-id-atom (atom {}))

(defn by-id []
  @by-id-atom)

(defmulti handle-event
  (fn [model event] (message/type event)))

(defmethod handle-event :default
  [model _]
  model)

(defmethod handle-event ::task-aggregate/Created
  [model {:keys [task-id description ::message/number]}]
  (assoc model task-id {:description description}))

(defmethod handle-event ::task-aggregate/Deleted
  [model {:keys [task-id]}]
  (dissoc model task-id))

(defmethod handle-event ::task-aggregate/DescriptionUpdated
  [model {:keys [task-id description ::message/number]}]
  (assoc-in model [task-id :description] description))

(defn setup! [event-store]
  (reset! store event-store)
  (let [c (event-channel event-store all-events-stream-id -1 false)]
    (go-loop []
      (when-let [event (<! c)]
        (swap! by-id-atom handle-event event)
        (recur)))))
