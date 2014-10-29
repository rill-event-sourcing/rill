(ns studyflow.learning.web.routes
  (:require [clout-link.defroute :refer [defroute]]))

(defroute update-course-material :put "/api/internal/course/:course-id")

(defroute update-entry-quiz-material :put "/api/internal/entry-quiz/:entry-quiz-id")

(defroute query-course-material :get "/api/course-material/:course-id/:student-id")

(defroute query-section :get "/api/course-material/:course-id/chapter/:chapter-id/section/:section-id")

(defroute query-question :get "/api/course-material/:course-id/chapter/:chapter-id/section/:section-id/question/:question-id")
(defroute query-chapter-quiz-question :get "/api/course-material/:course-id/chapter/:chapter-id/question/:question-id")

(defroute get-status :get "/status")
(defroute get-start :get "/")

(defroute get-course-page :get "/:course-name")

(defroute section-test-init :put "/api/section-test-init/:course-id/:section-id/:student-id")
(defroute section-test-reveal-worked-out-answer :put "/api/section-test-reveal-worked-out-answer/:section-id/:student-id/:course-id/:question-id")
(defroute section-test-check-answer :put "/api/section-test-check-answer/:section-id/:student-id/:course-id/:question-id")
(defroute section-test-next-question :put "/api/section-test-next-question/:section-id/:student-id/:course-id")

(defroute section-test-replay :get "/api/section-test-replay/:section-id/:student-id")

(defroute chapter-quiz-start :put "/api/chapter-quiz-start/:course-id/:chapter-id/:student-id")
(defroute chapter-quiz-submit-answer :put "/api/chapter-quiz-submit-answer/:course-id/:chapter-id/:student-id/:question-id")
(defroute chapter-quiz-dismiss-error-screen :put "/api/chapter-quiz-dismiss-error-screen/:course-id/:chapter-id/:student-id")
(defroute chapter-quiz-stop :put "/api/chapter-quiz-dismiss-error-screen/:course-id/:chapter-id/:student-id")

(defroute chapter-quiz-replay :get "/api/chapter-quiz-replay/:course-id/:chapter-id/:student-id")

(defroute entry-quiz-dismiss-nag-screen :put "/api/entry-quiz-dismiss-nag-screen/:course-id/:student-id")
(defroute entry-quiz-init :put "/api/entry-quiz-init/:course-id/:student-id")
(defroute entry-quiz-visit-first-question :put "/api/entry-quiz-visit-first-question/:course-id/:student-id")

(defroute entry-quiz-submit-answer :put "/api/entry-quiz-submit-answer/:course-id/:student-id")
(defroute entry-quiz-replay :get "/api/entry-quiz-replay/:course-id/:student-id")

(defroute tracking-navigation :put "/api/tracking/navigation/:student-id")
