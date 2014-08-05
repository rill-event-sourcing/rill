(ns studyflow.school-administration.web
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [compojure.route :refer [not-found]]
            [studyflow.school-administration.web.command :as command]
            [studyflow.school-administration.web.query :as query]
            [studyflow.school-administration.read-model :as m]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
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

(defn make-request-handler [event-store read-model]
  (-> (fn [{:keys [uri] :as req}]
        (if (= "/" uri)
          (redirect "/list-students")
          ((compojure/routes
            (fn [r]
              (catchup-handler @read-model))
            (command/commands-app event-store)
            (query/queries-app read-model)) req)))
      wrap-exception-catcher
      (wrap-defaults (assoc-in site-defaults [:static :resources] "school_administration/public"))))
