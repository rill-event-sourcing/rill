(ns studyflow.web.status
  (:require [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]))

(defn status
  [r]
  {:status 200
   :conten-type "text/html"
   :body (pr-str r)})

(def status-handler
  (handle routes/get-status
          status))

