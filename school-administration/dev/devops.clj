(ns devops
  (:require [rill.event-store :refer :all]
            [rill.message :as message :refer [primary-aggregate-id]]
            [rill.handler :refer [try-command]]
            [rill.aggregate :refer [load-aggregate]]
            [rill.repository :refer [retrieve-aggregate wrap-basic-repository]]
            [studyflow.school-administration.student.events :as events]
            [studyflow.school-administration.student :refer [release-email-address! claim-email-address!]]
            [rill.event-store.psql :refer [psql-event-store]]))

(defn get-repo
  [url]
  (-> (psql-event-store url)
      wrap-basic-repository))

(defn email-owner
  [repository email-address]
  (:current-owner-id (retrieve-aggregate repository (primary-aggregate-id (claim-email-address! "fake" email-address)))))

(defn release-email-address
  [repository email-address]
  (if-let [owner-id (email-owner repository email-address)]
    (if-let [owner (retrieve-aggregate repository owner-id)]
      (println "Won't release email owned by " owner)
      (try-command repository (release-email-address! owner-id email-address)))
    (println "Email not currently owned!")))
