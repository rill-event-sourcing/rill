(ns studyflow.school-administration.read-model.event-listener
  (:require [studyflow.school-administration.read-model.event-handler :refer [handle-event]]
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
        (if (not (instance? UnprocessableMessage event))
          (swap! model-atom handle-event event)
          (log/debug [:skipped-event]))
        (recur)))))
