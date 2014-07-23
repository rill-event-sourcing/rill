(ns studyflow.system.components.session-store
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car]))

(defprotocol ISessionStore
  (lookup-session [this session-id]))

(defrecord RedisSessionStore [config]
  ISessionStore
  (lookup-session [this session-id]
    (let [[student-uuid _] (car/wcar config
                                     (car/get session-id)
                                     ;; should get expire from config
                                     ;; if there is no key for session-id this is a noop
                                     (car/expire session-id (get config :session-max-age)))]
      student-uuid)))

(defn redis-session-store [config]
  (RedisSessionStore. (merge {:session-max-age 21600} config)))
