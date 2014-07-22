(ns studyflow.web.status
  (:require [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]))

(defn status
  [r]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str
          ;; temporary to guide people to the demo page
          "Course demo at: <a href=\"/course/71a2bd12-00b9-46fd-b52c-019ea4d2e3ea\">/course/71a2bd12-00b9-46fd-b52c-019ea4d2e3ea</a>"
          (pr-str r))})

(def status-handler
  (handle routes/get-status
          status))

