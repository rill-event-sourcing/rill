(ns studyflow.web.api
  (:require [studyflow.learning.read-model :as model]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn navigation-tree
  [course]
  (model/course-tree course))

(defn handlers
  [& fns]
  (fn [request]
    (some #(% request) fns)))

(defn handler
  [request]
  (-> (handlers navigation-tree)
      wrap-json-response))


