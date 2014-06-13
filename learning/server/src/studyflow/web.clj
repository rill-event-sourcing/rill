(ns studyflow.web
  (:require [studyflow.web.api :as api]
            [studyflow.web.status :as status]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]))

(defn make-request-handler
  [event-store read-model]
  (combine-ring-handlers
   (api/make-request-handler event-store read-model)
   status/status-handler))

