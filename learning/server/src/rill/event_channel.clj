(ns rill.event-channel
  (:require [clojure.core.async :refer [thread >!! chan]]
            [rill.event-store :as store]
            [clojure.tools.logging :as log]))

(def long-poll-seconds 20)

(defn push-event!!
  "push event to channel. ignores nil events.
returns false when we should not continue pushing."
  [ch event]
  (log/info event)
  (if event
    (boolean (>!! ch event))
    true))

(defn push-events!!
  [ch cursor events]
  (log/debug events)
  (loop [cursor cursor [event & events] events]
    (if event
      (if (push-event!! ch event)
        (recur (:cursor (meta events)) events)
        nil)
      cursor)))

(defn event-channel-listen!!
  "Push events from stream into ch. Blocking"
  [event-store stream-id cursor ch]
  (loop [cursor cursor]
    (log/debug [:listen!! stream-id cursor])
    (when-let [new-cursor (push-events!! ch cursor (store/retrieve-events-since event-store stream-id cursor long-poll-seconds))]
      (recur new-cursor))))

(defn event-channel
  "Start an event listener in a new thread and returns a channel
  containing the messages from the stream."
  [event-store stream-id from-version buf-or-n]
  (let [ch (chan buf-or-n)]
    (thread (do (log/info ["started event listen thread"])
                (event-channel-listen!! event-store stream-id from-version ch)))
    ch))
