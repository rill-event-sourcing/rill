(ns studyflow.components.redis-session-store
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [taoensso.carmine :as car :refer [wcar]]
            [studyflow.components.session-store :refer [SessionStore]]))

(defrecord RedisSessionStore [client]
  SessionStore

  (create-session [store user-id role session-max-age]
    (let [session-id (str (java.util.UUID/randomUUID))]
      (wcar client (car/set session-id {:user-id user-id :role role}))
      (wcar client (car/expire session-id session-max-age))
      session-id))

  (delete-session! [store session-id]
    (wcar client (car/del session-id)))

  (get-user-id [store session-id]
    (:user-id (wcar client (car/get session-id))))

  (get-role [store session-id]
    (:role (wcar client (car/get session-id)))))

(defn redis-session-store
  ([config]
     (->RedisSessionStore (merge {:pool {} :spec {}} config)))
  ([]
     (redis-session-store {})))

