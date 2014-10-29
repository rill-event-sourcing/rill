(ns studyflow.reporting.main
  (:require [clojure.core.async :refer [<!! thread chan >!! alts!!] :as async]
            [clojure.set :refer [join]]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.conversion :as cnv]
            [clojurewerkz.elastisch.native.document :as esd]
            [clojurewerkz.elastisch.native.index :as esi]
            [rill.event-channel :refer [event-channel]]
            [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message]
            [studyflow.migrations.active-migrations :refer [wrap-active-migrations]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce])
  (:import [org.elasticsearch.action.bulk BulkRequestBuilder BulkResponse BulkItemResponse])
  (:gen-class))

(defn -main [event-store-uri elastic-search-ip1 elastic-search-ip2 elastic-search-ip3]
  {:pre [event-store-uri elastic-search-ip1 elastic-search-ip2 elastic-search-ip3]}
  (let [conn (es/connect [[elastic-search-ip1 9300][elastic-search-ip2 9300][elastic-search-ip3 9300]] {"cluster.name" "studyflow"})]
    (println "Making clean ElasticSearch database...")
    (try (esi/delete conn "gibbon_reporting")
         (while (esi/exists? conn "gibbon_reporting")
           (log/info "Waiting until index is deleted")
           (Thread/sleep 1000))
         (catch Throwable e
           (println e)
           (log/error e "Could not delete gibbon_reporting index on ElasticSearch")))
    (esi/create conn "gibbon_reporting" :settings {"number_of_shards" 20})

    (let [store (-> (psql-event-store event-store-uri)
                    wrap-active-migrations)
          ch (event-channel store all-events-stream-id -1 0)
          es (chan 300)
          batches (async/partition 100 es)
          retries (chan 300)]

      ;; post to ES
      (thread (loop []
                (let [[batch _] (alts!! [retries batches] :priority true)]
                  (try
                    (log/info "Doing batch starting with id:  " (.id (first batch)))
                    (let [^BulkRequestBuilder bulk-request
                          (reduce
                           (fn [bulk-request request]
                             (.add bulk-request request))
                           (.prepareBulk conn)
                           batch)]
                      (let [^BulkResponse res (.. bulk-request execute actionGet)]
                        (when (.hasFailures res)
                          (doseq [[response request] (zipmap (.getItems res)
                                                             batch)
                                  :when (.isFailed response)]

                            ;; just requeue failed requests for posting
                            ;; to ES, ES doesn't require them to arrive
                            ;; in order
                            (log/error (.getFailureMessage response) " id: " (.id request))
                            (>!! retries [request]))
                          (log/error "failure during batch add"))))
                    (catch Throwable e
                      (println e)
                      (log/error e "Error on saving event to ElasticSearch database")
                      (doseq [request batch]
                        (>!! retries [request])))))
                (recur)))

      (println "Importing all events...")
      (loop []
        (when-let [event (<!! ch)]
          (do
            (if (= (message/type event) :rill.event-channel/CaughtUp)
              (do (println ",")
                  (println "caught up with event-store...")))

            (let [id (message/id event)
                  msg-type (message/type event)
                  ns-parts (split (namespace msg-type) #"\.")
                  short-ns (last (filter #(not= % "events") ns-parts))
                  nice-type (str short-ns "/" (name msg-type))
                  timestamp (time-coerce/from-date (message/timestamp event))]

              (if (or (= nice-type "course/Published")
                      (= nice-type "course/Updated"))
                (println " -> skipping course/Published or course/Updated event")
                (do
                  (>!! es
                       (cnv/->index-request "gibbon_reporting"
                                            nice-type
                                            (assoc event "@timestamp" timestamp)
                                            ;; id makes
                                            ;; this put-if-absent and
                                            ;; therefore retryable
                                            {:id (str id)}))))))
          (recur))))))
