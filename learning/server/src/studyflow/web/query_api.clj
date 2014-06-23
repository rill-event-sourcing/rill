(ns studyflow.web.query-api
  (:require [clojure.tools.logging :refer [info debug spy]]
            [clout-link.route :refer [handle]]
            [studyflow.learning.read-model.queries :as queries]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]
            [rill.uuid :refer [uuid]]
            [cheshire.core :as json]
            [studyflow.json-tools :refer [key-from-json]]
            [studyflow.learning.read-model :as model]))

(def query-handler
  "This handler returns data for the json api (or nil)"
  (combine-ring-handlers
   (handle routes/query-course-material
           (fn [{model :read-model {course-id :course-id :as params} :params}]
             (debug "Query handler for " course-id "with model: " model)
             (if-let [course  (queries/course-material model (uuid course-id))]
               {:status 200
                :body course}
               {:status 400})))
   (handle routes/query-section
           (fn [{model :read-model {:keys [course-id section-id] :as params} :params}]
             (debug "Query handler for " course-id " and " section-id "with model: " model)
             (if-let [section (queries/section model (uuid course-id) (uuid section-id))]
               {:status 200
                :body section}
               {:status 400})))))

(defn wrap-read-model
  [f read-model-atom]
  (fn [r]
    (f (assoc r :read-model @read-model-atom))))

(defn make-request-handler
  [read-model]
  (-> #'query-handler
      (wrap-read-model read-model)))
