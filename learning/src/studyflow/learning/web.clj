(ns studyflow.learning.web
  (:require [ring.util.response :as resp]
            [studyflow.learning.web.api :as api]
            [studyflow.learning.web.authentication :as authentication]
            [studyflow.learning.web.browser-resources :as browser-resources]
            [studyflow.web.caching :refer [wrap-no-cache-dwim]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.authentication :refer [wrap-cookie-domain wrap-redirect-urls]]
            [studyflow.learning.web.status :as status]
            [studyflow.learning.web.start :as start]
            [studyflow.learning.read-model :as m]))

(defn fallback-handler
  [r]
  (resp/not-found (str "Not found.\n" (pr-str r))))

(defn wrap-read-model
  [handler read-model-atom]
  (fn [request]
    (handler (assoc request :read-model @read-model-atom))))

(defn make-request-handler
  [event-store read-model session-store redirect-urls cookie-domain]
  (-> (combine-ring-handlers  browser-resources/resource-handler
                              (-> (combine-ring-handlers
                                   start/handler
                                   (api/make-request-handler event-store)
                                   (wrap-redirect-urls browser-resources/course-page-handler redirect-urls))
                                  (authentication/wrap-authentication session-store)
                                  (wrap-read-model read-model)
                                  (wrap-redirect-urls redirect-urls)
                                  (wrap-cookie-domain cookie-domain))
                              status/status-handler
                              fallback-handler)
      wrap-no-cache-dwim))