(ns rill.bench.writes
  (:require [rill.event-store :refer [append-events]]
            [rill.message :as message]
            [rill.uuid :refer [new-id]]
            [clojure.core.async :as async :refer [>!! <!! thread]]
            [criterium.core :refer [bench benchmark with-progress-reporting ]]
            [rill.event-store.memory :refer [memory-store]]
            [rill.event-store.psql :refer [psql-event-store]]
            [clojure.java.jdbc :as jdbc])
  (:gen-class))

(defn writer-thread
  [store num-streams channel]
  (let [stream-ids (vec (repeatedly num-streams new-id))]
    (async/thread
      (loop [[head :as chunk] (<!! channel)
             stream-index 0
             cursors (vec (repeat num-streams -1))]
        (when head
          (assert (append-events store (stream-ids stream-index) (cursors stream-index) chunk))
          (recur (<!! channel) (mod (inc stream-index) num-streams) (update-in cursors [stream-index] #(+ % (count chunk)))))))))

(defn random-message
  []
  {:numeric-prop (rand 1000)
   :string-prop "foobar-bla-bla-bla"
   :some-id (new-id)
   message/type ::RandomMessage
   message/id (new-id)
   message/timestamp (java.util.Date.)})

(defn do-writes
  "Write chunks of messages to store round-robin over num-writers
  threads, each writer appending its own streams"
  [store chunks num-writers buffer-size streams-per-writer]
  (let [out-chans (vec (repeatedly num-writers (partial async/chan buffer-size)))
        threads (mapv #(writer-thread store streams-per-writer %)
                      out-chans)]
    (try (loop [[chunk :as my-chunks] chunks
                writer-index 0]
           (when chunk
             (>!! (out-chans writer-index) chunk)
             (recur (next my-chunks) (mod (inc writer-index) num-writers))))
         ;; stop writers
         (finally
           (doseq [c out-chans]
             (async/close! c))
           ;; wait for writers to finish
           (doseq [t threads]
             (<!! t))))))

(defn clear-db!
  [spec]
  (jdbc/db-do-commands spec "TRUNCATE rill_events")
  (while (not= 0 (:count (first (jdbc/query spec "SELECT COUNT(*) AS count FROM rill_events"))))
    (Thread/sleep 50)))

(defn empty-psql-store
  [spec]
  (clear-db! spec)
  (psql-event-store spec))

(defn quick-write-bench
  [spec]
  (let [chunks (doall
                (partition-all 2 (repeatedly 1000 random-message)))]
    (with-progress-reporting (prn (bench (do-writes (empty-psql-store spec) chunks 10 5 3))))))


(defn -main
  [psql-spec]
  (quick-write-bench psql-spec))
