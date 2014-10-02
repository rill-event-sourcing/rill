(ns studyflow.learning.web.authentication
  (:require [clojure.tools.logging :as log]
            [ring.middleware.cookies :as cookies]
            [studyflow.web.authentication :refer [redirect-login wrap-check-cookie]]
            [studyflow.learning.read-model :as read-model]))

(defn wrap-student [handler]
  (fn [{:keys [read-model] :as req}]
    (if-let [student-id (get req :student-id)]
      (if-let [student (read-model/get-student read-model student-id)]
        (handler (assoc req :student (assoc student :student-id student-id)))
        (do
          (log/warn "Can't find student through session, perhaps re-logging in will work")
          (redirect-login req)))
      req)))

(defn get-student-id [{{:keys [user-id user-role]} :session}]
  (when (and user-id (= "student" user-role))
    user-id))

(defn wrap-student-id [handler]
  (fn [req]
    (if-let [student-id (get-student-id req)]
      (handler (-> req (assoc :student-id student-id)))
      (redirect-login req))))

(defn wrap-authentication [handler]
  (-> handler
      wrap-student
      wrap-student-id
      wrap-check-cookie))
