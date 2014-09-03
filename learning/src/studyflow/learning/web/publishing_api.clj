(ns studyflow.learning.web.publishing-api
  (:require [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [rill.web :refer [wrap-command-handler]]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.course-material :as course-material]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]
            [studyflow.learning.web.routes :as routes]))

;; load command handlers
(require 'studyflow.learning.course)
(require 'studyflow.learning.entry-quiz)

(def publish-course-handler
  (clout/handle
   routes/update-course-material
   (fn [{{:keys [course-id]} :params body :body}]
     (course-commands/publish! (uuid course-id)
                               (course-material/parse-course-material body)))))

(defn make-handler
  [event-store]
  (->  publish-course-handler
       (wrap-command-handler event-store)))

(defn make-request-handler
  [event-store]
  (-> (make-handler event-store)
      wrap-json-io))
