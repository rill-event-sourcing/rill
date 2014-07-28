(ns user
  (:require [clojure.tools.logging :refer [info debug spy]]
            [studyflow.school-administration.system :as sys]
            [studyflow.components.memory-event-store :refer [memory-event-store-component]]
            [com.stuartsierra.component :as component]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]
            [clojure.tools.namespace.repl :refer (refresh)])
  (:import [org.apache.log4j Logger]))

(def dev-config {:port 5000})

(defn dev-system [dev-options]
  (merge (sys/prod-system dev-options)
         {:event-store (component/using
                        (memory-event-store-component)
                        [])}))

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
