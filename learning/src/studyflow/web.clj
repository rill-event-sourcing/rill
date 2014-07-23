(ns studyflow.web
  (:require [ring.util.response :as resp]
            [studyflow.web.api :as api]
            [studyflow.web.authentication :as authentication]
            [studyflow.web.browser-resources :as browser-resources]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.logging :refer [wrap-logging]]
            [studyflow.web.status :as status]))

(defn fallback-handler
  [r]
  (resp/not-found (str "Not found.\n" (pr-str r))))

(defn make-request-handler
  [event-store read-model session-store]
  (-> (combine-ring-handlers
       (-> (combine-ring-handlers
            (api/make-request-handler event-store read-model)
            (browser-resources/make-request-handler))
           (authentication/wrap-authentication read-model session-store))
       status/status-handler
       fallback-handler)
      wrap-logging))
