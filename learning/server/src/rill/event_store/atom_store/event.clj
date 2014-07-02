(ns rill.event-store.atom-store.event
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [rill.event-store.atom-store.feed :as feed]
            [rill.message :as message]
            [miner.tagged :as tagged])
  (:import (com.fasterxml.jackson.core JsonParseException)))


(defn event->entry
  [event]
  {:eventId (str (message/id event))
   :eventType (name (message/type event))
   :data {:edn (pr-str event)}})

(defrecord UnprocessableMessage [v])

(defn entry->event
  [entry]
  (or (try (if-let [data (:edn (:data entry))]
             (when (string? data)
               (-> (tagged/read-string data)
                   (assoc message/id (:eventId entry)
                          message/type (:eventType entry)
                          message/number (:eventNumber entry)))))
           (catch RuntimeException _
             nil))
      (->UnprocessableMessage entry)))

(defn post
  [store-uri stream-id expected-version events opts]
  (http/post (feed/head-uri store-uri stream-id)
             (merge opts
                    {:body (json/generate-string (map event->entry events))
                     :content-type :json
                     :headers {"ES-ExpectedVersion" (str expected-version)}})))

(defn body->event
  [body]
  (entry->event (get-in body [:content] {})))

(defn load-event
  [uri opts]
  (if-let [response (try (http/get uri (merge opts {:as :json}))
                         (catch JsonParseException e
                           nil))]
    (body->event (:body response))))
