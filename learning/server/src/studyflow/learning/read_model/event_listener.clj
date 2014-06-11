(ns studyflow.learning.read-model.event-listener
  (:require [rill.event-store :as store]))

(defn update-model
  [a event-store]
  ()
  )

(defn listen
  [initial-model event-store]
  (let [a (atom initial-model)]
    (store/retrieve-events-since )
    )
  )
