(ns studyflow.web.api
  (:require [studyflow.web.api.command :as command-api]
            [studyflow.web.api.query :as query-api]
            [studyflow.web.api.replay :as replay-api]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]))

(defn make-request-handler
  [event-store read-model]
  (-> (combine-ring-handlers
       (replay-api/make-request-handler event-store)
       (query-api/make-request-handler read-model)
       (command-api/make-request-handler event-store))
      wrap-json-io))
