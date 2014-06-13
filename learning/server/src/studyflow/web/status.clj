(ns studyflow.web.status
  (:require [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]))

(def status-handler
  (handle routes/get-status
          (fn [r]
            {:status 200
             :conten-type "text/html"
             :body (pr-str r)})))

