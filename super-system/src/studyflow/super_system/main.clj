(ns studyflow.super-system.main
  (:require [clojure.tools.logging :as log]
            [studyflow.super-system :as system]
            [com.stuartsierra.component :refer [start]])
  (:gen-class))

(defn -main
  []
  (-> (system/make-system {})
      start))
