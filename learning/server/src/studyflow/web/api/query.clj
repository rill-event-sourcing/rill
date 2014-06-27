(ns studyflow.web.api.query
  (:require [clojure.tools.logging :refer [debug]]
            [clout-link.route :as clout]
            [rill.uuid :refer [uuid]]
            [studyflow.learning.read-model.queries :as queries]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.routes :as routes]))

(def handler
  "This handler returns data for the json api (or nil)"
  (combine-ring-handlers
   (clout/handle routes/query-course-material
                 (fn [{model :read-model {course-id :course-id} :params}]
                   (debug "Query handler for " course-id "with model: " model)
                   (if-let [course  (queries/course-material model (uuid course-id))]
                     {:status 200 :body course}
                     {:status 400})))
   (clout/handle routes/query-section
                 (fn [{model :read-model {:keys [course-id section-id]} :params}]
                   (debug "Query handler for " course-id " and " section-id "with model: " model)
                   (if-let [section (queries/section model (uuid course-id) (uuid section-id))]
                     {:status 200 :body section}
                     {:status 400})))))

(defn wrap-read-model
  [f read-model-atom]
  (fn [r]
    (f (assoc r :read-model @read-model-atom))))

(defn make-request-handler
  [read-model]
  (-> handler
      (wrap-read-model read-model)))
