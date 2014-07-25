(ns studyflow.web.api.replay
  "Fetch old events"
  (:require [studyflow.learning.section-test.replay :refer [replay-section-test]]
            [clout-link.route :as clout]
            [studyflow.web.authorization :as authorization]
            [studyflow.web.routes :as routes]
            [rill.uuid :refer [uuid]]))

(defn response
  [{:keys [events aggregate-id aggregate-version]}]
  (if (seq events)
    {:status 200
     :body {:events events
            :aggregate-id aggregate-id
            :aggregate-version aggregate-version}}
    {:status 401}))

(def handler
  (clout/handle routes/section-test-replay
                (authorization/wrap-student-authorization
                 (fn [{{:keys [section-id student-id]} :params store :event-store :as request}]
                   (response (replay-section-test store section-id student-id))))))

(defn make-request-handler
  [event-store]
  (fn [request]
    (#'handler (assoc request :event-store event-store))))

