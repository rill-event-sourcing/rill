(ns user
  (:require [studyflow.system :as sys]
            [com.stuartsierra.component :as component]
            [clojure.test :as test :refer [run-all-tests]]
            [clojure.tools.trace :refer [trace trace-ns]]))

(defonce system nil)

(defn start []
  (alter-var-root #'system (constantly (-> (sys/prod-system sys/prod-config)
                                           (component/start))))
  :started)

(defn stop []
  (alter-var-root #'system (constantly (component/stop #'system))))

(defn reset []
  (when system
    (stop))
  (start))

