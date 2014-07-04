(ns user
  (:require [clojure.tools.logging :refer [info debug spy]]
            [studyflow.system :as sys]
            [studyflow.web :as web]
            [com.stuartsierra.component :as component]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]
            [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.string :as string]
            [ring.mock.request :as ring-mock]
            [studyflow.web.routes :as routes]
            [clout-link.route :as route])
  (:import [org.apache.log4j Logger]))

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

(defrecord DevRingHandlerComponent [event-store read-model]
  component/Lifecycle
  (start [component]
    (info "Starting handler")
    (assoc component :handler
           (-> (web/make-request-handler (:store event-store) (:read-model read-model))
               (wrap-dev-cljs
                "<script src=\"/public/js/studyflow.js\" type=\"text/javascript\"></script>"
                "<script src=\"/dev/public/js/react_0.9.0_local_copy.js\" type=\"text/javascript\"></script>
                 <script src=\"/public/js/studyflow-dev.js\" type=\"text/javascript\"></script>
                 <script type=\"text/javascript\">goog.require('studyflow.web.core');</script>"))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn dev-ring-handler-component []
  (map->DevRingHandlerComponent {}))


(defrecord FixturesLoadingComponent [ring-handler]
  component/Lifecycle
  (start [component]
    (info "Starting fixtures-loading-component")
    (let [handler (:handler ring-handler)
          materials (slurp "test/studyflow/material.json")
          course-id (let [[_ rest] (.split ^String materials ":")
                          [quoted-id] (.split ^String rest ",")
                          [_ id] (.split ^String quoted-id "\"")]
                      id)]
      (handler (-> (ring-mock/request :put (route/uri-for routes/update-course-material course-id)
                                      materials)
                   (ring-mock/content-type "application/json"))))
    component)
  (stop [component]
    (info "Stopping fixtures-loading-component, not removing anything")
    component))

(defn fixtures-loading-component []
  (map->FixturesLoadingComponent {}))

(def dev-config (merge sys/prod-config
                       {}))

(defn dev-system [dev-options]
  (merge (sys/prod-system dev-options)
         {:ring-handler (component/using
                         (dev-ring-handler-component)
                         [:event-store :read-model])
          :fixtures-loading (component/using
                             (fixtures-loading-component)
                             [:ring-handler])}))

;; from cascalog playground for swank/slime
(defn bootstrap-emacs []
  (let [logger (Logger/getRootLogger)]
    (doto (. logger (getAppender "stdout"))
      (.setWriter *out*))
    (alter-var-root #'clojure.test/*test-out* (constantly *out*))
    (info "Logging to repl")))

(defonce system nil)

(defn init []
  (alter-var-root #'system (constantly (dev-system dev-config))))

(defn start []
  (bootstrap-emacs)
  (alter-var-root #'system component/start)
  :started)

(defn stop []
  (alter-var-root #'system
                  (fn [s]
                    (info "stopping system")
                    (info "system is" s)
                    (when s
                      (component/stop s)))))

(defn go []
  (bootstrap-emacs)
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
