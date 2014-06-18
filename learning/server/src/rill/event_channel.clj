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
  [ch events]
  (loop [[event & events] events]
    (if (seq events)
      (if (push-event!! ch event)
        (recur events)
        false)
      true)))

(defn event-channel-listen!!
  "Push events from stream into ch. Blocking"
  [event-store stream-id from-version ch]
  (loop [version from-version]
    (log/debug [:listen!! stream-id version])
    (let [events (store/retrieve-events-since event-store stream-id version long-poll-seconds)]
      (log/debug ["recieved" (count events) "events"])
      (when (push-events!! ch events)
        (recur (+ version (count events)))))))

(defn event-channel
  "Start an event listener in a new thread and returns a channel
  containing the messages from the stream."
  [event-store stream-id from-version buf-or-n]
  (let [ch (chan buf-or-n)]
    (thread (do (log/info ["started event listen thread"])
                (event-channel-listen!! event-store stream-id from-version ch)))
    ch))
