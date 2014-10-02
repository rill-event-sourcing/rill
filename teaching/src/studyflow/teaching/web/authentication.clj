(ns studyflow.teaching.web.authentication
  (:require [clojure.tools.logging :as log]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.web.authentication :refer [wrap-check-cookie redirect-login]]))

(defn wrap-teacher [handler]
  (fn [{:keys [read-model] :as req}]
    (if-let [teacher-id (get req :teacher-id)]
      (if-let [teacher (read-model/get-teacher read-model teacher-id)]
        (handler (assoc req :teacher (assoc teacher :teacher-id teacher-id)))
        (do
          (log/warn "Can't find teacher through session, perhaps re-logging in will work")
          (redirect-login req)))
      req)))

(defn get-teacher-id [{{:keys [user-id user-role]} :session}]
  (when (and user-id (= user-role "teacher"))
    user-id))

(defn wrap-teacher-id [handler]
  (fn [req]
    (if-let [teacher-id (get-teacher-id req)]
      (handler (-> req (assoc :teacher-id teacher-id)))
      (redirect-login req))))

(defn wrap-authentication [handler]
  (-> handler
      wrap-teacher
      wrap-teacher-id
      wrap-check-cookie))

