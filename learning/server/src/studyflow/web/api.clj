(ns studyflow.web.api
  (:require [studyflow.learning.read-model :as model]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.command-handler :as cmd]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]
            [rill.handler :as es-dispatcher]
            [ring.middleware.logger :as logger :refer [wrap-with-plaintext-logger]]
            [clojure.tools.logging :as log]
            [rill.uuid :refer [new-id]]))



(defn command-routes
  [event-store & route-command-matchers]
  (fn [request]
    (when-let [command (some #(% request) route-command-matchers)]
      (log/info [:command command])
      (if (= :es-dispatcher/error (es-dispatcher/try-command event-store command))
        {:status 500}
        {:status 200}))))

(defn wrap-middleware
  [f]
  (-> f
      wrap-json-response
      wrap-json-body
     wrap-with-plaintext-logger))

(defn make-request-handler
  [event-store]
  (-> (command-routes event-store
                      (handle routes/update-course-material
                              (fn [{{:keys [course-id]} :params body :body}]
                                (studyflow.learning.commands/->UpdateCourse! (new-id) course-id nil (material/parse-course-material body)))))
      wrap-middleware))
