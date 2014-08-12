(ns studyflow.school-administration.web
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [compojure.route :refer [not-found]]
            [studyflow.school-administration.web.command :as command]
            [studyflow.school-administration.web.query :as query]
            [studyflow.school-administration.read-model :as m]
            [ring.middleware.defaults :refer [secure-site-defaults site-defaults wrap-defaults]]
            [ring.util.response :refer [redirect resource-response]]))

(defn wrap-exception-catcher [app]
  (fn [req]
    (try (app req)
         (catch Throwable e
           (log/error e)
           (-> (resource-response "public/500.html")
               (assoc :status 500)
               (assoc-in [:headers "Content-Type"] "text/html"))))))

(defn catchup-handler
  [read-model]
  (when-not (m/caught-up? read-model)
    {:status 503
     :body "Server starting up."
     :headers {"Content-Type" "text/plain"}}))


(defn studyflow-site-config [config]
  (-> config
      (assoc-in [:session :cookie-name] "studyflow_school_session")
      (assoc-in [:security :anti-forgery] false)
      (assoc-in [:static :resources] "school_administration/public")
      (assoc-in [:security :ssl-redirect] false)))

(defn make-request-handler [secure-site-defaults? event-store read-model]
  (-> (fn [{:keys [uri] :as req}]
        (if (= "/" uri)
          (redirect "/list-students")
          ((compojure/routes
            (fn [r]
              (catchup-handler @read-model))
            (command/commands-app event-store)
            (query/queries-app read-model)) req)))
      wrap-exception-catcher
      (wrap-defaults (studyflow-site-config (if secure-site-defaults?
                                              secure-site-defaults
                                              site-defaults)))))
