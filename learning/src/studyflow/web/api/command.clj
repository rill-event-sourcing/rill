(ns studyflow.web.api.command
  (:require [clojure.tools.logging :as log]
            [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [studyflow.learning.course-material :as course-material]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.section-test.commands :as section-test-commands]
            [rill.web :refer [wrap-command-handler]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]))

;; Load command handlers
(require 'studyflow.learning.course)
(require 'studyflow.learning.section-test)

(def handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-handler` to actually run the
commands."
  (combine-ring-handlers
   (clout/handle
    routes/update-course-material
    (fn [{{:keys [course-id]} :params body :body}]
      (course-commands/publish! (uuid course-id)
                                (course-material/parse-course-material body))))

   (clout/handle
    routes/section-test-init
    (fn [{{:keys [section-test-id section-id course-id]} :params :as params}]
      (section-test-commands/init! section-test-id
                                   (uuid section-id)
                                   (uuid course-id))))

   (clout/handle
    routes/section-test-check-answer
    (fn [{{:keys [section-test-id section-id course-id question-id]} :params body :body}]
      (let [inputs body]
        (section-test-commands/check-answer! section-test-id
                                             (uuid section-id)
                                             (uuid course-id)
                                             (uuid question-id)
                                             inputs))))
      (clout/handle
    routes/section-test-next-question
    (fn [{{:keys [section-test-id section-id course-id]} :params body :body}]
      (section-test-commands/next-question! section-test-id
                                            (uuid section-id)
                                            (uuid course-id))))))

(defn make-request-handler
  [event-store]
  (-> handler
      (wrap-command-handler event-store)))
