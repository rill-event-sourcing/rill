(ns rill.event-channel
  (:require [clojure.core.async :refer [thread >!! chan]]
            [rill.event-store :as store]
            [rill.message :refer [defevent]]
            [clojure.tools.logging :as log]
            [schema.core :as s])
  (:import (java.util Date)))

(def long-poll-seconds 20)

(defn push-event!!
  "push event to channel.
  returns false when we should not continue pushing."
  [ch event]
  (boolean (>!! ch event)))

(defn push-events!!  [ch cursor events]
  (loop [cursor cursor
         [event & events] events]
    (if event
      (if (push-event!! ch event)
        (recur (:cursor (meta event)) events)
        nil)
      cursor)))

;; This is the event you get from a channel when it has
;; caught up with the current head of the event stream

(defevent CaughtUp
  :timestamp s/Inst)

(defn event-channel-listen!!
  "Push events from stream into ch. Blocking"
  [event-store stream-id cursor ch]
  (loop [cursor cursor at-head false]
    (when-let [new-cursor (push-events!! ch cursor (store/retrieve-events-since event-store stream-id cursor long-poll-seconds))]
      (when (not at-head)
        (push-event!! ch (caught-up (Date.)))
        (log/info "Caught up with head of event stream."))
      (recur new-cursor true))))

(defn event-channel
  "Start an event listener in a new thread and returns a channel
  containing the messages from the stream."
  [event-store stream-id from-version buf-or-n]
  (let [ch (chan buf-or-n)]
    (thread (try
              (do (log/info ["started event listen thread"])
                  (event-channel-listen!! event-store stream-id from-version ch))
              (catch Exception e
                (log/error e "Exception in event channel listener")
                (throw e))))
    ch))
