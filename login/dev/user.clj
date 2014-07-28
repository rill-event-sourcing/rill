(ns user
  (:require [clojure.tools.logging :as log]
            [studyflow.login.system :as system]
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

(defn init []
  (alter-var-root #'system (constantly (system/make-system {:jetty-port 4000
                                                            :default-redirect-paths {"student" "http://localhost:3000"
                                                                                     "editor" "http://localhost:2000"}
                                                            :session-max-age (* 8 60 60)
                                                            :cookie-domain nil}))))

(defn start []
  (bootstrap-emacs)
  (alter-var-root #'system component/start)
  :started)

(defn stop []
  (alter-var-root #'system
                  (fn [s]
                    (log/info "stopping system")
                    (log/info "system is" s)
                    (when s
                      (component/stop s)))))

(defn go []
  (bootstrap-emacs)
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
