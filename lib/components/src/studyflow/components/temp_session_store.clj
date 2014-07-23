(ns studyflow.components.temp-session-store
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [studyflow.components.session-store :refer [SessionStore]]))

(defrecord TempSessionStore [sessions]
  SessionStore

  (create-session [store user-id role session-max-age]
    (let [session-id (str (java.util.UUID/randomUUID))]
      (swap! sessions assoc session-id {:user-id user-id :role role})
      session-id))

  (delete-session! [store session-id]
    (swap! sessions dissoc session-id))

  (get-user-id [store session-id]
    (:user-id (get @sessions session-id)))

  (get-role [store session-id]
    (:role (get @sessions session-id))))

(defn temp-session-store []
  (->TempSessionStore (atom {})))
