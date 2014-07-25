(ns studyflow.web.authorization
  (:require [clojure.tools.logging :as log]))

(defn wrap-student-authorization [handler]
  (fn [req]
    (let [student-id-param (get-in req [:params :student-id])
          student-id-auth-uuid (get-in req [:student :student-id])]
      (if (and student-id-param
               student-id-auth-uuid
               (= student-id-param (str student-id-auth-uuid)))
        (handler req)
        (do
          (log/warn "Student not authorized" student-id-param student-id-auth-uuid)
          {:status 403
           :body "Not authorized."})))))
