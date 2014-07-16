(ns studyflow.learning.read-model.event-listener
  (:require [studyflow.learning.read-model.event-handler :refer [handle-event]]
            [studyflow.loop-tools :refer [while-let]]
            [clojure.core.async :refer [<!! thread]]
            [rill.event-store.atom-store]
            [clojure.tools.logging :as log])
  (:import (rill.event_store.atom_store.event UnprocessableMessage)))

(defn listen!
  "listen on event-channel"
  [model-atom event-channel]
  (log/info "Starting read-model event listener")
  (thread
    (log/info "Started read-model event listener")
    
    (while-let [e (<!! event-channel)]
               (if (not (instance? UnprocessableMessage e))
                 (do (log/info e)
                     (swap! model-atom handle-event e))
                 (log/debug [:skipped-event])))))
