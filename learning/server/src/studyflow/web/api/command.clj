(ns studyflow.web.api.command
  (:require [clout-link.route :as clout]
            [rill.uuid :refer [new-id uuid]]
            [studyflow.learning.commands :as commands]
            [studyflow.learning.course-material :as material]
            [studyflow.web.command-executor :refer [wrap-command-executor]]
            [studyflow.web.routes :as routes]))

;; Load command handlers
(require 'studyflow.learning.command-handler)

(def handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-executor` to actually run the
commands."
  (clout/handle
   routes/update-course-material
   (fn [{{:keys [course-id]} :params body :body}]
     (commands/->PublishCourse! (new-id) (uuid course-id)
                                (material/parse-course-material body)))))

(defn make-request-handler
  [event-store]
  (-> handler
      (wrap-command-executor event-store)))
