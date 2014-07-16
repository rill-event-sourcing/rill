(ns studyflow.web.browser-resources
  (:require [clout-link.route :as clout]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.file-info :as file-info]
            [studyflow.web.routes :as routes]))

(defn make-request-handler
  []
  (-> (clout/handle routes/get-course-page
                    (constantly (resource-response "templates/courses.html")))
      (wrap-resource "/")
      file-info/wrap-file-info))
