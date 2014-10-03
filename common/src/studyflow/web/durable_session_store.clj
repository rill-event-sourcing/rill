(ns studyflow.web.durable-session-store
  (:require [clj-redis-session.core :as redis]))

(defn durable-store
  [session-store-url]
  (redis/redis-store {:pool {} :spec {:uri session-store-url}}
                     {:expire-secs (* 3600 12)
                      :reset-on-read true}))

