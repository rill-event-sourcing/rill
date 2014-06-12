(ns studyflow.system
  (:require [rill.event-store.atom-store :as atom-store]
            [studyflow.web.api :as web-api]
            [environ.core :refer [env]]))

(defonce event-store nil)
(defonce web-handler nil)

(defn init-event-store
  []
  (alter-var-root #'event-store (constantly (atom-store/atom-event-store (or (env :event-store-uri) "http://127.0.0.1:2113")))))

(defn init-web-handler
  []
  (alter-var-root #'web-handler (constantly (web-api/make-request-handler event-store))))

(defn init
  []
  (init-event-store)
  (init-web-handler))

