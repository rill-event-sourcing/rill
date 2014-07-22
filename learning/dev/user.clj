(ns user
  (:require [clojure.tools.logging :refer [info debug spy]]
            [com.stuartsierra.component :as component]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]
            [clojure.tools.namespace.repl :refer (refresh)]
            [learning-dev-system :as learning-dev-system])
  (:import [org.apache.log4j Logger]))

;; from cascalog playground for swank/slime
(defn bootstrap-emacs []
  (let [logger (Logger/getRootLogger)]
    (doto (. logger (getAppender "stdout"))
      (.setWriter *out*))
    (alter-var-root #'clojure.test/*test-out* (constantly *out*))
    (info "Logging to repl")))

(defonce system nil)

(defn init []
  (alter-var-root #'system (constantly (learning-dev-system/dev-system learning-dev-system/dev-config))))

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
