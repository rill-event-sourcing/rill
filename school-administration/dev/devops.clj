(ns devops
  (:require [rill.event-store :refer :all]
            [rill.event-stream :refer [all-events-stream-id]]
            [rill.message :as message :refer [primary-aggregate-id]]
            [rill.handler :refer [try-command]]
            [rill.aggregate :refer [load-aggregate]]
            [rill.repository :refer [retrieve-aggregate wrap-basic-repository]]
            [studyflow.migrations.active-migrations :refer [wrap-active-migrations]]
            [studyflow.credentials.email-ownership :as email]
            [studyflow.credentials.edu-route-id-ownership :as edu-route]
            [rill.event-store.psql :refer [psql-event-store]]))

(defn get-repo
  [url]
  (-> (psql-event-store url)
      wrap-active-migrations
      wrap-basic-repository))

(defn email-owner
  [repository email-address]
  (:current-owner-id (retrieve-aggregate repository (primary-aggregate-id (email/claim! "fake" email-address)))))

(defn edu-route-owner
  [repository edu-route-id]
  (:current-owner-id (retrieve-aggregate repository (primary-aggregate-id (edu-route/claim! edu-route-id "fake")))))

(defn release-email-address
  [repository email-address]
  (if-let [owner-id (email-owner repository email-address)]
    (if-let [owner (retrieve-aggregate repository owner-id)]
      (println "Won't release email owned by " owner)
      (try-command repository (email/release! owner-id email-address)))
    (println "Email not currently owned!")))

(defn release-edu-route-id
  [repository edu-route-id]
  (if-let [owner-id (edu-route-owner repository edu-route-id)]
    (if-let [owner (retrieve-aggregate repository owner-id)]
      (println "Won't release id owned by " owner)
      (try-command repository (edu-route/release! edu-route-id owner-id)))
    (println "Id " edu-route-id " not currently owned!")))



(defn claim-events
  [r]
  (map (fn [e]
         (prn (message/timestamp e))
         e)
       (filter (fn [e]
                 (case (message/type e)
                   :studyflow.credentials.edu-route-id-ownership.events/Claimed
                   true
                   :studyflow.credentials.edu-route-id-ownership.events/Released
                   true
                   false))
               (retrieve-events r all-events-stream-id))))

(defn dangling
  [repo events]
  (filter (fn [c]
            (and (= (message/type c)
                    :studyflow.credentials.edu-route-id-ownership.events/Claimed)
                 (not (seq (retrieve-events repo (:owner-id c))))))
          events))

