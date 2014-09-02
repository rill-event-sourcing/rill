(ns studyflow.web.authentication
  (:require [clojure.tools.logging :as log]
            [ring.middleware.cookies :as cookies]))

(defn redirect-login
  [{:keys [uri cookie-domain] {:keys [learning login]} :redirect-urls}]
  {:status 302
   :headers {"Location" login}
   :cookies (if cookie-domain
              {:studyflow_session {:value ""
                                   :domain cookie-domain
                                   :path "/"
                                   :max-age -1}}
              {:studyflow_session {:value ""
                                   :path "/"
                                   :max-age -1}})})

(defn wrap-check-cookie [handler]
  (fn [req]
    (if-let [session-id (get-in req [:cookies "studyflow_session" :value])]
      (handler (assoc req :session-id session-id))
      (redirect-login req))))


