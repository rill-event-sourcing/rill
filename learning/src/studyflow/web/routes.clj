(ns studyflow.web.routes
  (:require [clout-link.defroute :refer [defroute]]))

(defroute update-course-material :put "/api/internal/course/:course-id")

(defroute query-course-material :get "/api/course-material/:course-id/:student-id")

(defroute query-section :get "/api/course-material/:course-id/chapter/:chapter-id/section/:section-id")

(defroute query-question :get "/api/course-material/:course-id/chapter/:chapter-id/section/:section-id/question/:question-id")

(defroute get-status :get "/status")
(defroute get-start :get "/")

(defroute get-course-page :get "/:course-name")

(defroute section-test-init :put "/api/section-test-init/:course-id/:section-id/:student-id")
(defroute section-test-reveal-worked-out-answer :put "/api/section-test-reveal-worked-out-answer/:section-id/:student-id/:course-id/:question-id")
(defroute section-test-check-answer :put "/api/section-test-check-answer/:section-id/:student-id/:course-id/:question-id")
(defroute section-test-next-question :put "/api/section-test-next-question/:section-id/:student-id/:course-id")

(defroute section-test-replay :get "/api/section-test-replay/:section-id/:student-id")

