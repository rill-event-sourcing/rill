(ns studyflow.web.api
  (:require [studyflow.web.command-api :as command-api]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]
            [studyflow.web.logging :refer [wrap-logging]]
            [studyflow.web.query-api :as query-api]
            [studyflow.web.browser-resources :as browser-resources]))

(defn wrap-middleware
  [f]
  (-> f
      wrap-json-io
      wrap-logging))

(defn make-request-handler
  [event-store read-model]
  (-> (combine-ring-handlers (query-api/make-request-handler read-model)
                             (command-api/make-request-handler event-store)
                             (browser-resources/make-request-handler read-model))
      wrap-middleware))
