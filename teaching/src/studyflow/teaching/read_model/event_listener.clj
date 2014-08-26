(ns studyflow.teaching.read-model.event-listener
  (:require [studyflow.teaching.read-model.event-handler :refer [handle-event]]
            [clojure.core.async :refer [<!! thread]]
            [rill.event-store.atom-store]
            [clojure.tools.logging :as log])
  (:import (rill.event_store.atom_store.event UnprocessableMessage)))

(defn listen!
  "listen on event-channel"
  [model-atom event-channel]
  (thread
    (loop []
      (when-let [event (<!! event-channel)]
        (when (not (instance? UnprocessableMessage event))
          (swap! model-atom handle-event event))
        (recur)))))
