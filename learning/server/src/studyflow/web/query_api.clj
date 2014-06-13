(ns studyflow.web.query-api
  (:require [clout-link.route :refer [handle]]
            [studyflow.learning.read-model.queries :as queries]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]
            [rill.uuid :refer [uuid]]))

(def query-handler
  "This handler returns data for the json api (or nil)"
  (combine-ring-handlers
   (handle routes/query-course-material
           (fn [{model :read-model {course-id :course-id :as params} :params}]
             (queries/course-material model (uuid course-id))))))

(defn wrap-read-model
  [f read-model-atom]
  (fn [r]
    (f (assoc r :read-model @read-model-atom))))

(defn make-request-handler
  [read-model]
  (-> #'query-handler
      (wrap-read-model read-model)))
