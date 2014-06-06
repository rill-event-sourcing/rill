(ns studyflow.web.logging
  (:require [ring.middleware.logger :as logger]
            [ring.middleware.stacktrace :as trace]))

(defn wrap-logging
  [f]
  (-> f
      trace/wrap-stacktrace-web
      logger/wrap-with-plaintext-logger))
