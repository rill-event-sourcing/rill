(ns studyflow.web.status
  (:require [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle]]))

(defn status
  [r]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str
          ;; temporary to guide people to the demo page
          "Course demo at: <a href=\"/course/1f8e2f2b-ed71-4788-8f08-5529949f2188\">/course/1f8e2f2b-ed71-4788-8f08-5529949f2188</a>"
          (pr-str r))})

(def status-handler
  (handle routes/get-status
          status))

