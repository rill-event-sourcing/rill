(ns studyflow.teaching.web
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [studyflow.teaching.web.query :as query]
            [studyflow.teaching.read-model :as m]
            [ring.util.response :refer [redirect]]))

(defn catchup-handler
  [{:keys [read-model]}]
  (when-not (m/caught-up? read-model)
    {:status 503
     :body "Server starting up."
     :headers {"Content-Type" "text/plain", "Refresh" "5"}}))

(defn wrap-read-model
  [app read-model-atom]
  (fn [req] (app (assoc req :read-model @read-model-atom))))

(defn app
  [{:keys [uri] :as req}]
  (if (= "/" uri)
    (redirect "/reports/")
    ((compojure/routes
      catchup-handler
      query/app) req)))
