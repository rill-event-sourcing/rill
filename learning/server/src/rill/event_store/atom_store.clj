(ns rill.event-store.atom-store
  "This is an event store implementation using the GetEventstore.com store as a backend.

Code largely taken from
https://github.com/jankronquist/rock-paper-scissors-in-clojure/tree/master/eventstore/src/com/jayway/rps
"
  (:require [rill.event-store :as store]
            [rill.message :as msg]
            [rill.event-stream :as stream]
            [clj-http.client :as client]
            [cheshire.core :as json]))

(defn to-eventstore-format [event]
  {:eventId (msg/id event)
   :eventType (.replace (name (msg/type event)) \. \_)
   :data (assoc (msg/data event) :id (msg/id event))})

(defn uri-for-relation [relation links]
  (:uri (first (filter #(= relation (:relation %)) links))))

(defn load-event [uri message-constructor]
  (let [response (client/get uri {:as :json})
        event-data (get-in response [:body :content :data] {})
        event-type (.replace (get-in response [:body :content :eventType]) \_ \.)]
    (if event-type
      (message-constructor event-type event-data)
      event-data)))

(declare load-events)

(defn load-events-from-list [response message-constructor]
  (let [body (:body response)
        links (:links body)
        event-uris (reverse (map :id (:entries body)))
        previous-uri (uri-for-relation "previous" links)]
    (lazy-cat (map #(load-event % message-constructor) event-uris)
              (if previous-uri (lazy-seq (load-events previous-uri message-constructor))))))

(defn load-events-from-page [page message-constructor]
  (let [links (:links page)
        event-uris (reverse (map :id (:entries page)))]
    (map #(load-event % message-constructor) event-uris)))

(defn load-events [uri message-constructor]
  (load-events-from-list (client/get uri {:as :json}) message-constructor))

(defn forward-page-uri
  [feed-uri from page-size]
  (format "%s/%d/forward/%d" feed-uri (if (= from -1) 0 from) page-size))

(defn load-event-page
  [feed-uri from page-size poll-seconds]
  (let [response (client/get (forward-page-uri feed-uri from page-size)
                             (merge {:as :json
                                     :throw-exceptions false}
                                    (if (and poll-seconds
                                             (< 0 poll-seconds))
                                      {"ES-LongPoll" (str poll-seconds)})))]
    (if-not (= 200 (:status response))
      nil
      (:body response))))

(defn load-events-from-feed [uri from-version message-constructor wait-for-seconds]
  (let [page (load-event-page uri from-version 20 wait-for-seconds)]
    (if-not page
      stream/empty-stream
      (if-let [previous-uri (uri-for-relation "previous" (:links page))]
        (concat (load-events-from-page page message-constructor) (load-events previous-uri message-constructor))
        (load-events-from-page page message-constructor)))))

(defn atom-event-store
  ([uri message-constructor]
     (letfn [(stream-uri [stream-id] (str uri "/streams/" stream-id))]
       (reify store/EventStore
         (retrieve-events-since [this stream-id from-version wait-for-seconds]
           (load-events-from-feed (stream-uri stream-id) from-version message-constructor wait-for-seconds))

         (append-events
           [this stream-id from-version events]
           (client/post (stream-uri stream-id)
                        {:body (json/generate-string (map to-eventstore-format events))
                         :content-type :json
                         :headers {"ES-ExpectedVersion" (str (if (< 1 from-version)
                                                               (dec from-version)
                                                               from-version))}})))))
  ([uri]
     (atom-event-store uri msg/strict-map->Message)))
