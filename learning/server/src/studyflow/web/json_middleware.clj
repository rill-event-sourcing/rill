(ns studyflow.web.json-middleware
  (:require [studyflow.json-tools :refer [key-to-json key-from-json]]
            [ring.middleware.json :as ring]))

(defn wrap-json-body
  [f]
  (ring/wrap-json-body f {:keywords? key-from-json}))

(defn wrap-json-response
  [f]
  (ring/wrap-json-response f {:key-fn key-to-json}))

(defn wrap-json-io
  [f]
  (-> f
      wrap-json-body
      wrap-json-response))
