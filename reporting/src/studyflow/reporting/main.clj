(ns studyflow.reporting.main
  (:require [clojure.core.async :refer [<!!]]
            [clojure.set :refer [join]]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.document :as esd]
            [clojurewerkz.elastisch.native.index :as esi]
            [rill.event-channel :refer [event-channel]]
            [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message]
            [studyflow.migrations.active-migrations :refer [wrap-active-migrations]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce])
  (:gen-class))

(defn -main [event-store-uri elastic-search-ip]
  {:pre [event-store-uri elastic-search-ip]}
  (let [store (-> (psql-event-store event-store-uri)
                  wrap-active-migrations)
        ch (event-channel store all-events-stream-id -1 0)
        conn (es/connect [[elastic-search-ip 9300]] {"cluster.name" "elasticsearch"})]

    (println "Making clean ElasticSearch database...")
    (try (esi/delete conn "gibbon_reporting")
         (catch Throwable e
           (println e)
           (log/error e "Could not delete gibbon_reporting index on ElasticSearch")))

    (esi/create conn "gibbon_reporting" :settings {"number_of_shards" 20})

    (println "Importing all events...")
    (loop []
      (when-let [event (<!! ch)]
        (do
          (if (= (message/type event) :rill.event-channel/CaughtUp)
            (do (println ",")
                (println "caught up with event-store...")))

          (let [msg-type (message/type event)
                ns-parts (split (namespace msg-type) #"\.")
                short-ns (last (filter #(not= % "events") ns-parts))
                nice-type (str short-ns "/" (name msg-type))
                timestamp (time-coerce/from-date (message/timestamp event))
                from_date (time/minus (time/now) (time/days 28))]

            (when
                (time/after? timestamp from_date)
              (if (= nice-type "course/Updated")
                (println " -> skipping course/Updated event")
                (do
                  (println (str timestamp " => " nice-type))
                  (try (esd/create conn "gibbon_reporting" nice-type (assoc event "@timestamp" timestamp))
                       (catch Throwable e
                         (println e)
                         (log/error e "Error on saving event to ElasticSearch database"))))))))
        (recur)))))
