(ns studyflow.web.browser-resources
  (:require [clojure.tools.logging :refer [info debug spy]]
            [clout-link.route :refer [handle]]
            [studyflow.learning.read-model.queries :as queries]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]
            [rill.uuid :refer [uuid]]
            [clojure.java.io :as io]
            [ring.middleware.resource :as resource]))


(defn course-page-handler
  [{model :read-model {course-id :course-id :as params} :params}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "templates/courses.html"))})

(defn wrap-read-model
  [f read-model-atom]
  (fn [r]
    (f (assoc r :read-model @read-model-atom))))

(defn make-request-handler
  [read-model]
  (-> (combine-ring-handlers
       (handle routes/get-course-page
               course-page-handler))
      (wrap-read-model read-model)
      (resource/wrap-resource "/")))
