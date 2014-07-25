(ns studyflow.web.api.command
  (:require [clojure.tools.logging :as log]
            [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [studyflow.learning.section-test.commands :as section-test-commands]
            [rill.web :refer [wrap-command-handler]]
            [studyflow.web.authorization :as authorization]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]))

;; Load command handlers
(require 'studyflow.learning.section-test)

(def handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-handler` to actually run the
commands."
  (combine-ring-handlers
   (clout/handle
    routes/section-test-init
    (authorization/wrap-student-authorization
     (fn [{{:keys [section-id student-id section-id course-id]} :params :as params}]
       (section-test-commands/init! (uuid section-id)
                                    (uuid student-id)
                                    (uuid course-id)))))

   (clout/handle
    routes/section-test-check-answer
    (authorization/wrap-student-authorization
     (fn [{{:keys [section-id student-id course-id question-id]} :params body :body}]
       (let [{:keys [expected-version inputs]} body]
         (section-test-commands/check-answer! (uuid section-id)
                                              (uuid student-id)
                                              expected-version
                                              (uuid course-id)
                                              (uuid question-id)
                                              inputs)))))
   (clout/handle
    routes/section-test-next-question
    (authorization/wrap-student-authorization
     (fn [{{:keys [section-id student-id course-id]} :params
           {:keys [expected-version] :as body} :body}]
       (section-test-commands/next-question! (uuid section-id)
                                             (uuid student-id)
                                             expected-version
                                             (uuid course-id)))))))

(defn make-request-handler
  [event-store]
  (-> handler
      (wrap-command-handler event-store)))
