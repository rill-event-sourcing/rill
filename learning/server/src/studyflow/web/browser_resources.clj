(ns studyflow.web.browser-resources
  (:require [clout-link.route :refer [handle]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]))

(defn make-request-handler
  [read-model]
  (-> (combine-ring-handlers
       (handle routes/get-course-page
               (constantly
                (resource-response "templates/courses.html"))))
      (wrap-resource "/")))
