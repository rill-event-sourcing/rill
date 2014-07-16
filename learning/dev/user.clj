(ns user
  (:require [clojure.tools.logging :refer [info debug spy]]
            [studyflow.system :as sys]
            [studyflow.system.components.memory-event-store :refer [memory-event-store-component]]
            [studyflow.system.components.dev-ring-handler :refer [dev-ring-handler-component]]
            [studyflow.system.components.fixtures-loading :refer [fixtures-loading-component]]
            [studyflow.web :as web]
            [com.stuartsierra.component :as component]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]
            [clojure.tools.namespace.repl :refer (refresh)])
  (:import [org.apache.log4j Logger]))

(def dev-config (merge sys/prod-config
                       {}))

(defn dev-system [dev-options]
  (merge (sys/prod-system dev-options)
         {:ring-handler (component/using
                         (dev-ring-handler-component)
                         [:event-store :read-model])
          :event-store (component/using
                        (memory-event-store-component)
                        [])
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
