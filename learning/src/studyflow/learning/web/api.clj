(ns studyflow.learning.web.api
  (:require [studyflow.learning.web.api.command :as command-api]
            [studyflow.learning.web.api.query :as query-api]
            [studyflow.learning.web.api.replay :as replay-api]
            [studyflow.learning.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.learning.web.caching :refer [wrap-no-caching]]
            [studyflow.learning.web.json-middleware :refer [wrap-json-io]]))


(defn make-request-handler
  [event-store]
  (-> (combine-ring-handlers
       (replay-api/make-request-handler event-store)
       query-api/handler
       (command-api/make-request-handler event-store))
      wrap-no-caching
      wrap-json-io))
