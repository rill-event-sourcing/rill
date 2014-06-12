(ns studyflow.system
  (:require [rill.event-store.atom-store :as atom-store]
            [studyflow.web.api :as web-api]
            [studyflow.learning.read-model.event-listener :refer [listen!]]
            [studyflow.learning.read-model :refer [empty-model]]
            [rill.event-channel :refer [event-channel]]
            [environ.core :refer [env]]))

(defonce event-store nil)
(defonce web-handler nil)

(defonce event-listener nil)
(defonce channel nil)
(defonce read-model nil)

(defn init-event-store
  []
  (alter-var-root #'event-store (constantly (atom-store/atom-event-store (or (env :event-store-uri) "http://127.0.0.1:2113")))))


(defn init-event-listener
  []
  (when-not event-store
    (throw (Exception. "No event store initialized")))
  (alter-var-root #'read-model (constantly (atom empty-model)))
  (alter-var-root #'channel (constantly (event-channel event-store "$all" 0 0)))
  (alter-var-root #'event-listener (constantly (listen! read-model channel))))


(defn init-web-handler
  []
  (alter-var-root #'web-handler (constantly (web-api/make-request-handler event-store read-model))))

(defn init
  []
  (init-event-store)
  (init-event-listener)
  (init-web-handler))

