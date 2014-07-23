(ns studyflow.system.components.dev-ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.web :as web]
            [clojure.string :as string]
            [clojure.tools.logging :refer [info debug spy]]))

(defn wrap-dev-cljs [handler match replace]
  (fn [req]
    (let [res (handler req)]
      (if (and (.startsWith (get-in res [:headers "Content-Type"] "") "text/html" )
               (not (.contains ^String (str "" (get req :query-string)) "prod")))
        (-> res
            (update-in [:body]
                       (fn [body]
                         (-> body
                             (cond->
                              (not (string? body))
                              slurp)
                             (string/replace match replace))))
            (update-in [:headers] dissoc "Content-Length" "Last-Modified"))
        res))))

(defrecord DevRingHandlerComponent [event-store read-model session-store redirect-urls]
  Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler
           (-> (web/make-request-handler (:store event-store) (:read-model read-model) session-store redirect-urls)
               (wrap-dev-cljs
                "<script src=\"/js/studyflow.js\" type=\"text/javascript\"></script>"
                "<script src=\"/js/react_0.9.0_local_copy.js\" type=\"text/javascript\"></script>
                 <script src=\"/js/studyflow-dev.js\" type=\"text/javascript\"></script>
                 <script type=\"text/javascript\">goog.require('studyflow.web.core');</script>"))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn dev-ring-handler-component [redirect-urls]
  (map->DevRingHandlerComponent {:redirect-urls redirect-urls}))
