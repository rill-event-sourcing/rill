(ns user
  (:require [clojure.tools.logging :as log]
            [studyflow.super-system :as super-system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh]])
  (:import [org.apache.log4j Logger]))


;; from cascalog playground for swank/slime
(defn bootstrap-emacs []
  (let [logger (Logger/getRootLogger)]
    (doto (. logger (getAppender "stdout"))
      (.setWriter *out*))
    (alter-var-root #'clojure.test/*test-out* (constantly *out*))
    (log/info "Logging to repl")))

(defonce system nil)

(defn init [config]
  (alter-var-root #'system (constantly (super-system/make-system config))))

(defn start []
  (bootstrap-emacs)
  (alter-var-root #'system component/start)
  :started)

(defn stop []
  (alter-var-root #'system
                  (fn [s]
                    (log/info "stopping system")
                    (when s
                      (component/stop s)))))

(defn go
  ([config]
     (bootstrap-emacs)
     (init config)
     (start)
     :started)
  ([]
     (go {})))

(defn reset []
  (stop)
  (refresh :after 'user/go))






