(ns rill.event-store.atom-store.event
  (:require [clj-http.client :as http]
            [rill.event-store.atom-store [feed :as feed]]
            [cheshire.core :as json])
  (:import com.fasterxml.jackson.core.JsonParseException))

(defn body->event
  [body]
  (get-in body [:content :data] {}))

(defn load-event
  [uri opts]
  (if-let [response (try (http/get uri (merge opts {:as :json}))
                         (catch JsonParseException e
                           nil))]
    (body->event (:body response))))

(defn event->entry
  [event]
  {:eventId (:id event)
   :eventType (:type event)
   :data event})

(defn post
  [store-uri stream-id expected-version events opts]
  (http/post (feed/head-uri store-uri stream-id)
             (merge opts
                    {:body (json/generate-string (map event->entry events))
                     :content-type :json
                     :headers {"ES-ExpectedVersion" (str expected-version)}})))
