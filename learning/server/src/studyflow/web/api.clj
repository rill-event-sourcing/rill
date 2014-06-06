(ns studyflow.web.api
  (:require [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.command-handler :as cmd]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]
            [rill.handler :as es-dispatcher]
            [studyflow.web.logging :refer [wrap-logging]]
            [clojure.tools.logging :as log]
            [rill.uuid :refer [new-id]]))

(defn wrap-command-executor
  "Given a set of ring handler that returns a command (or nil), execute
  the command with the given event store and return status 500 or 200"
  [ring-handler event-store]
  (fn [request]
    (when-let [command (ring-handler request)]
      (log/info ["Executing command" (class command)])
      (if (= :es-dispatcher/error (es-dispatcher/try-command event-store command))
        {:status 500}
        {:status 200}))))

(defn wrap-middleware
  [f]
  (-> f
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-logging))

(defn combine-ring-handlers
  [& handlers]
  (fn [r]
    (some #(% r) handlers)))

(def command-ring-handler
  "This handler matches ring requests and returns a command (or nil) for the given request.
Intended to be wrapped by `wrap-command-executor` to actually run the
commands."
  (combine-ring-handlers
   (handle routes/update-course-material
           (fn [{{:keys [course-id]} :params body :body :as request}]
             (studyflow.learning.commands/->UpdateCourse! (new-id) course-id (material/parse-course-material body))))))

(defn make-request-handler
  [event-store]
  (-> #'command-ring-handler
      (wrap-command-executor event-store)
      wrap-middleware))
