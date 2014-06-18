(ns rill.event-store.atom-store
  "This is an event store implementation using the GetEventstore.com store as a backend.

Code largely taken from
https://github.com/jankronquist/rock-paper-scissors-in-clojure/tree/master/eventstore/src/com/jayway/rps
"
  (:require [rill.event-store :as store]
            [rill.message :as msg]
            [rill.event-stream :as stream]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defn to-eventstore-format [event]
  {:eventId (msg/id event)
   :eventType (.replace (name (msg/type event)) \. \_)
   :data (assoc (msg/data event) :id (msg/id event))})

(defrecord Cursor [page-uri offset])

(defn with-cursor
  [event cursor]
  (with-meta event {:cursor cursor}))

(defn uri-for-relation [relation links]
  (:uri (first (filter #(= relation (:relation %)) links))))

(defn load-event [uri message-constructor]
  (let [response (client/get uri {:as :json})
        event-data (get-in response [:body :content :data] {})
        event-type (.replace (get-in response [:body :content :eventType]) \_ \.)]
    (if event-type
      (message-constructor event-type event-data)
      event-data)))

(defn load-page
  ([page-uri poll-seconds]
     (log/debug (str "Loading event page " page-uri))
     (let [response (client/get page-uri
                                (merge {:as :json
                                        :throw-exceptions false}
                                       (if (and poll-seconds
                                                (< 0 poll-seconds))
                                         {:headers {"ES-LongPoll" (str poll-seconds)}})))]
       (if-not (= 200 (:status response))
         nil
         (:body response))))
  ([page-uri]
     (load-page page-uri nil)))

(defn events-from-page
  [{:keys [offset page-uri]} page message-constructor]
  (let [links (:links page)
        event-uris (reverse (map :id (:entries page)))]
    (map-indexed
     (fn [this-offset e-uri]
       (with-cursor
         (load-event e-uri message-constructor)
         (->Cursor page-uri
                   (+ 1 offset this-offset))))
     (if (= offset -1)
       event-uris
       (drop (inc offset) event-uris)))))

(defn load-events
  [{:keys [page-uri] :as cursor} message-constructor wait-for-seconds]
  (log/debug "load-events" cursor)
  (if-let [page (load-page page-uri wait-for-seconds)]
    (let [events (events-from-page cursor page message-constructor)]
      (if-let [previous-uri (uri-for-relation "previous" (:links page))]
        (if (seq events)
          (concat events (lazy-seq (load-events (->Cursor previous-uri -1) message-constructor 0)))
          (load-events (->Cursor previous-uri -1) message-constructor wait-for-seconds))
        events))
    stream/empty-stream))

(defn load-from-head
  [stream-uri message-constructor wait-for-seconds]
  (if-let [page (load-page stream-uri wait-for-seconds)]
    (if-let [last-uri (uri-for-relation "last" (:links page))]
      (load-events (->Cursor last-uri -1) message-constructor 0) ;; start from last page
      (events-from-page (->Cursor stream-uri -1) page message-constructor))  ;; only one page
    stream/empty-stream))

(defrecord UnprocessableMessage [v])

(defn safe-convert
  [s m]
  (try
    (msg/strict-map->Message s m)
    (catch RuntimeException e
      (log/info "Ignoring malformed event" e)
      (->UnprocessableMessage m))))

(defn atom-event-store
  ([uri message-constructor]
     (letfn [(stream-uri [stream-id] (str uri "/streams/" stream-id))]
       (reify store/EventStore
         (retrieve-events-since [this stream-id cursor wait-for-seconds]
           (if (= -1 cursor)
             (load-from-head (stream-uri stream-id) message-constructor wait-for-seconds)
             (load-events (cond
                           (instance? Cursor cursor)
                           cursor

                           (:cursor (meta cursor))
                           (:cursor (meta cursor))
                           :else
                           (throw (RuntimeException. (str "Invalid cursor " (pr-str cursor)))))

                          message-constructor
                          wait-for-seconds)))

         (append-events
           [this stream-id from-version events]
           (log/debug [:appending (count events) :to stream-id :at from-version])
           (client/post (stream-uri stream-id)
                        {:body (json/generate-string (map to-eventstore-format events))
                         :content-type :json
                         :headers {"ES-ExpectedVersion" (str from-version)}})))))
  ([uri]
     (atom-event-store uri safe-convert)))
