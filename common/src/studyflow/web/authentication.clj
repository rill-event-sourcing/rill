(ns studyflow.web.authentication
  (:require [clojure.tools.logging :as log]
            [ring.middleware.cookies :as cookies]))

(defn redirect-login
  [{:keys [uri cookie-domain] {:keys [learning login]} :redirect-urls}]
  {:status 303
   :headers {"Location" login}
   :session nil})

(defn wrap-check-cookie [handler]
  (fn [{:keys [session] :as req}]
    (if session
      (handler req)
      (redirect-login req))))

(defn wrap-redirect-urls
  [f urls]
  (fn [r]
    (f (assoc r :redirect-urls urls))))


