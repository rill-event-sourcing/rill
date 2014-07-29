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

(defn wrap-redirect-urls [handler redirect-urls]
  (fn [req]
    (handler (assoc req :redirect-urls redirect-urls))))

(defn make-request-handler
  [event-store read-model session-store redirect-urls]
  (-> (combine-ring-handlers
       (-> (combine-ring-handlers
            (api/make-request-handler event-store read-model)
            (wrap-redirect-urls (browser-resources/make-request-handler) redirect-urls))
           (authentication/wrap-authentication read-model session-store)
           (wrap-redirect-urls redirect-urls))
       status/status-handler
       fallback-handler)
      wrap-logging))
