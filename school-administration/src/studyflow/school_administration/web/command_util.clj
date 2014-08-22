(ns studyflow.school-administration.web.command-util
  (:require [rill.handler :refer [try-command]]
            [rill.message :refer [primary-aggregate-id]]))

(defn merge-flash [resp m]
  (update-in resp [:flash] merge m))

(defn result->response [[status events-or-reason new-version] success error params]
  (case status
    :ok
    (merge-flash success
                 {:aggregate-version new-version
                  :aggregate-id (primary-aggregate-id (first events-or-reason))})

    (:conflict :out-of-date)
    (merge-flash error
                 {:warning "already edited by somebody else"})

    :rejected
    (merge-flash error
                 {:warning "command rejected"
                  :errors events-or-reason
                  :post-params params})))
