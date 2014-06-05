(ns studyflow.web.routes
  (:require [clout-link.defroute :refer [defroute]]))

(defroute update-course-material :put "/api/internal/course/:id")


