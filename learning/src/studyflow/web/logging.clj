(ns studyflow.web.logging
  (:require [ring.middleware.stacktrace :as trace]))

(defn wrap-logging
  [f]
  (-> f
      trace/wrap-stacktrace-web))
