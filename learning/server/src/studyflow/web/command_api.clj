(ns studyflow.web.command-api
  (:require [clout-link.route :refer [handle]]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.commands :as commands]
            [studyflow.learning.course-material :as material]
            [studyflow.web.command-executor :refer [wrap-command-executor]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]
            [clojure.tools.logging :as log]))

(def command-ring-handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-executor` to actually run the
commands."
  (combine-ring-handlers
   (handle routes/update-course-material
           (fn [{{:keys [course-id]} :params body :body :as request}]
             (commands/->UpdateCourse! (new-id) course-id (material/parse-course-material body))))))

(defn make-request-handler
  [event-store]
  (-> #'command-ring-handler
      (wrap-command-executor event-store)))
