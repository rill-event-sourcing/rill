(ns studyflow.web.browser-resources
  (:require [clout-link.route :as clout]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [studyflow.web.routes :as routes]))

(defn make-request-handler
  []
  (-> (clout/handle routes/get-course-page
                    (constantly (resource-response "templates/courses.html")))
      (wrap-resource "/")))
