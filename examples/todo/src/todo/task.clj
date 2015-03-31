(ns todo.task
  (:require [clojure.core.async :refer [chan go-loop mult put! tap untap <! <!!]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [todo.aggregates.task :as task-aggregate]
            [todo.commands.task :as task-command])
  (:import [java.util UUID]))

(defonce store-atom (atom nil))

(defn create! [description]
  (try-command @store-atom
               (task-command/create! (UUID/randomUUID) description)))

(defn update-description! [uuid description]
  (try-command @store-atom
               (task-command/update-description! uuid description)))

(defn delete! [uuid]
  (try-command @store-atom
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

(defonce out-mult-atom (atom nil))

(defn setup! [event-store]
  (reset! store-atom event-store)
  (let [in (event-channel event-store all-events-stream-id -1 false)
        out (chan)]
    (reset! out-mult-atom (mult out))
    (go-loop []
      (when-let [event (<! in)]
        (swap! by-id-atom handle-event event)
        (put! out event)
        (recur)))))

(defn wait-for! [in events]
  (loop [events (->> events (map ::message/id) set )]
    (when-let [event (<!! in)]
      (let [remaining-events (disj (:message/id event))]
        (when (seq remaining-events)
          (recur remaining-events))))))

(defn sync-command [command & args]
  (let [in (chan)]
    (tap @out-mult-atom in)
    (let [[status result] (apply command args)]
      (when (= status :ok) (wait-for! in result))
      (untap @out-mult-atom in)
      [status result])))
