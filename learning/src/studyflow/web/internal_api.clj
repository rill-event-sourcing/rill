(ns studyflow.web.internal-api
  (:require [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [rill.web :refer [wrap-command-handler]]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.course-material :as course-material]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]
            [studyflow.web.routes :as routes]))

;; load command handlers
(require 'studyflow.learning.course)

(def publish-course
  (clout/handle
   routes/update-course-material
   (fn [{{:keys [course-id]} :params body :body}]
     (course-commands/publish! (uuid course-id)
                               (course-material/parse-course-material body)))))

(defn make-request-handler
  [event-store]
  (-> (combine-ring-handlers publish-course)
      (wrap-command-handler event-store)
      wrap-json-io))
