(ns user
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [ring.mock.request :refer [request]]
            [studyflow.json-tools :refer [key-from-json]]
            [studyflow.super-system :as super-system]
            [rill.handler :refer [try-command]]
            [studyflow.learning.course.commands :as course]
            [studyflow.learning.course]
            [rhizome.viz :as viz])
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


(defn update-material!
  [path]
  (let [material (json/parse-string (slurp (io/file path)) key-from-json)
        store (:store (:event-store system))]
    (try-command store (course/publish! (:id material) material))))



(defn view-transitions
  [transitions]
  (viz/view-graph (distinct (apply concat (keys transitions) (map vals (vals transitions))))
                  #(distinct (vals (transitions %)))
                  :node->descriptor (fn [n] {:label n})
                  :edge->descriptor (fn [s d]
                                      {:label (string/join ", " (filter #(= d (get-in transitions [s %])) (keys (transitions s))))})))

