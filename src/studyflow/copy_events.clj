(ns studyflow.copy-events
  (:require [rill.event-store.atom-store :refer [atom-event-store]]
            [rill.event-store.psql :refer [psql-event-store]]
            [rill.event-store :refer [append-events]]
            [rill.event-channel :refer [event-channel]]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message]
            [rill.event-store.atom-store.event :refer [unprocessable?]]
            [studyflow.school-administration.school.events]
            [studyflow.school-administration.student.events]
            [studyflow.school-administration.department.events]
            [studyflow.login.edu-route-student.events]
            [studyflow.learning.section-test.events]
            [studyflow.learning.course.events]
            [clojure.core.async :refer [<!!]])
  (:gen-class))

(defn put-event
  [store e]
  (append-events store (message/primary-aggregate-id e) -2 [e]))

(defn copy
  [in-url user password out-url]
  {:pre [in-url user password out-url]}
  (let [in-ch (event-channel (atom-event-store in-url {:user user :password password})
                             all-events-stream-id -1 0)
        out-store (psql-event-store out-url)]
    (println "Printing all events...")
    (loop []
      (if-let [e (<!! in-ch)]
        (do (if (unprocessable? e)
              (println "Skipped message.")
              (put-event out-store e))
            (recur))))))

(defn -main
  [& args]
  (apply copy args))

