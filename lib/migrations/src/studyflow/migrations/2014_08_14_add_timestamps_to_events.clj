(ns studyflow.migrations.2014-08-14-add-timestamps-to-events
  (:require [rill.event-store :refer [retrieve-events append-events]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message])
  (:import java.util.Date))

(def start-timestamp-at #inst "2014-08-14T00:00:00.000-00:00")

(defn add-seconds
  [date num-seconds]
  (Date. (+ (.getTime date) (* 1000 num-seconds))))

(defn add-timestamp
  [event from]
  (if (message/timestamp event)
    event
    (assoc event message/timestamp (add-seconds from (message/number event)))))

(defn add-timestamps-to-events
  [in-event-store out-event-store timestamps-from]
  (doseq [[e :as events] (partition-by message/primary-aggregate-id (retrieve-events in-event-store all-events-stream-id))]
    (append-events out-event-store (message/primary-aggregate-id e) (dec (message/number e))
                   (map #(add-timestamp % timestamps-from) events))))

