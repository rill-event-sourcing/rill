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

(defn wrap-student-id [handler]
  (fn [{{:keys [user-id user-role]} :session :as req}]
    (if (and user-id
             (or (= "student" user-role)
                 (= "teacher" user-role)))
      (handler (assoc req
                 :student-id user-id
                 :user-role (keyword user-role)))
      (redirect-login req))))

(defn wrap-authentication [handler]
  (-> handler
      wrap-student
      wrap-student-id
      wrap-check-cookie))
