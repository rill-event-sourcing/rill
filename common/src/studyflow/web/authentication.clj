(ns studyflow.web.authentication
  (:require [clojure.tools.logging :as log]
            [ring.middleware.cookies :as cookies]))

(defn redirect-login
  [{:keys [uri cookie-domain] {:keys [learning login]} :redirect-urls}]
  {:status 303
   :headers {"Location" login}
   :cookies (if cookie-domain
              {:studyflow_session {:value "deleted"
                                   :domain cookie-domain
                                   :path "/"
                                   :max-age -1}}
              {:studyflow_session {:value "deleted"
                                   :path "/"
                                   :max-age -1}})})

(defn wrap-check-cookie [handler]
  (fn [req]
    (if-let [session-id (get-in req [:cookies "studyflow_session" :value])]
      (handler (assoc req :session-id session-id))
      (redirect-login req))))


(defn wrap-redirect-urls
  [f urls]
  (fn [r]
    (f (assoc r :redirect-urls urls))))

(defn wrap-cookie-domain
  [handler cookie-domain]
  (fn [request]
    (handler (assoc request :cookie-domain cookie-domain))))

