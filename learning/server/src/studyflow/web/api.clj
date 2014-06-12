(ns studyflow.web.api
  (:require [clout-link.route :refer [handle]]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.commands :as commands]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.read-model :as read-model]
            [studyflow.web.command-executor :refer [wrap-command-executor]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.json-middleware :refer [wrap-json-io]]
            [studyflow.web.logging :refer [wrap-logging]]
            [studyflow.web.routes :as routes]))

(defn wrap-middleware
  [f]
  (-> f
      wrap-json-io
      wrap-logging))

(def command-ring-handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-executor` to actually run the
commands."
  (combine-ring-handlers
   (handle routes/update-course-material
           (fn [{{:keys [course-id]} :params body :body :as request}]
             (commands/->UpdateCourse! (new-id) course-id (material/parse-course-material body))))))

(def query-handler
  "This handler returns data for the json api (or nil)"
  (combine-ring-handlers
   (handle routes/query-course-material
           (fn [{model :read-model {course-id :course-id} :params}]
             (read-model/course-tree model course-id)))))

(defn wrap-read-model
  [f read-model-atom]
  (fn [r]
    (f (assoc r :read-model @read-model-atom))))

(defn make-request-handler
  [event-store read-model]
  (-> #'command-ring-handler
      (wrap-command-executor event-store)
      query-handler
      (wrap-read-model read-model)
      wrap-middleware))
