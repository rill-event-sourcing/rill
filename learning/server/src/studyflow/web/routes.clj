(ns studyflow.web.routes
  (:require [clout-link.defroute :refer [defroute]]))

(defroute update-course-material :put "/api/internal/course/:course-id")

(defroute query-course-material :get "/api/course-material/:course-id")

(defroute query-section :get "/api/course-material/:course-id/chapter/:chapter-id/section/:section-id")

(defroute get-status :get "/")

(defroute get-course-page :get "/course/:course-id")
