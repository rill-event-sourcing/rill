(ns studyflow.web.publishing-api
  (:require [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [rill.web :refer [wrap-command-handler]]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.course-material :as course-material]
            [studyflow.learning.entry-quiz.commands :as entry-quiz-commands]
            [studyflow.learning.entry-quiz-material :as entry-quiz-material]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]
            [studyflow.web.routes :as routes]))

;; load command handlers
(require 'studyflow.learning.course)
(require 'studyflow.learning.entry-quiz)

(def publish-course-handler
  (clout/handle
   routes/update-course-material
   (fn [{{:keys [course-id]} :params body :body}]
     (course-commands/publish! (uuid course-id)
                               (course-material/parse-course-material body)))))

(def publish-entry-quiz-handler
  (clout/handle
   routes/update-entry-quiz-material
   (fn [{{:keys [entry-quiz-id]} :params body :body}]
     (entry-quiz-commands/publish! (uuid entry-quiz-id)
                                   (entry-quiz-material/parse-entry-quiz-material body)))))

(defn make-handler
  [event-store]
  (-> (combine-ring-handlers publish-course-handler
                             publish-entry-quiz-handler)
      (wrap-command-handler event-store)))

(defn make-request-handler
  [event-store]
  (-> (make-handler event-store)
      wrap-json-io))
