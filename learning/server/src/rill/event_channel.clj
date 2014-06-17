(ns rill.event-channel
  (:require [clojure.core.async :refer [thread >!! chan]]
            [rill.event-store :as store]
            [clojure.tools.logging :as log]))

(def long-poll-seconds 20)

(defn event-channel-listen!!
  "Push events from stream into ch. Blocking"
  [event-store stream-id from-version ch]
  (log/debug [:listen!! stream-id from-version])
  (let [[continue? to-version]
        (reduce
         (fn [[continue? version] event]
           (log/info event)
           (if event
             (if (>!! ch event)
               [true (inc version)]
               [false version])
             [true version]))
         [true from-version]
         (store/retrieve-events-since event-store stream-id from-version long-poll-seconds))]
    (when continue?
      (recur event-store stream-id to-version ch))))

(defn event-channel
  "Start an event listener in a new thread and returns a channel
  containing the messages from the stream."
  [event-store stream-id from-version buf-or-n]
  (let [ch (chan buf-or-n)]
    (thread (do (log/info ["started event listen thread"])
                (event-channel-listen!! event-store stream-id from-version ch)))
    ch))
