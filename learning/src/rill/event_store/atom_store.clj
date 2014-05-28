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
   :data (msg/data event)})

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

(defn load-events [uri message-constructor]
  (load-events-from-list (client/get uri {:as :json}) message-constructor))

;; three cases:
;; 1) stream does not exist
;; 2) stream exists, but has only a single page
;; 3) stream exists and has multiple pages

(defn load-events-from-feed [uri message-constructor]
  (let [response (client/get uri {:as :json :throw-exceptions false})]
    (if-not (= 200 (:status response))
      stream/empty-stream ; case 1
      (let [body (:body response)
            links (:links body)
            last-link (uri-for-relation "last" links)
            events (if last-link
                     (load-events last-link message-constructor) ; case 3
                     (load-events-from-list response message-constructor))] ; case 2
        (stream/->EventStream (dec (count events)) ;; TODO: improve
                                                   ;; efficiency of
                                                   ;; this - get
                                                   ;; version from
                                                   ;; latest event
                              events)))))

(defn atom-event-store
  ([uri message-constructor]
     (letfn [(stream-uri [aggregate-id] (str uri "/streams/" aggregate-id))]
       (reify store/EventStore
         (retrieve-event-stream [this aggregate-id]
           (load-events-from-feed (stream-uri aggregate-id) message-constructor))

         (append-events
           [this aggregate-id previous-event-stream events]
           (client/post (stream-uri aggregate-id)
                        {:body (json/generate-string (map to-eventstore-format events))
                         :content-type :json
                         :headers {"ES-ExpectedVersion" (str (:version (or previous-event-stream stream/empty-stream)))}})))))
  ([uri]
     (atom-event-store uri msg/map->Message)))

