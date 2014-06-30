(ns studyflow.web
  (:require [ring.util.response :as resp]
            [studyflow.web.api :as api]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.logging :refer [wrap-logging]]
            [studyflow.web.status :as status]))

(defn fallback-handler
  [r]
  (resp/not-found (str "Not found.\n" (pr-str r))))

(defn make-request-handler
  [event-store read-model]
  (-> (combine-ring-handlers
       (api/make-request-handler event-store read-model)
       status/status-handler
       fallback-handler)
      wrap-logging))
