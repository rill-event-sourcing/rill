(ns studyflow.web.browser-resources
  (:require [clout-link.route :as clout]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :as file-info]
            [studyflow.web.routes :as routes]))

(html/deftemplate course-frame "learning/templates/courses.html"
  [student login-url]
  [:input#student-full-name] (html/set-attr :value (:full-name student))
  [:input#student-id] (html/set-attr :value (str (:student-id student)))
  [:input#logout-target] (html/set-attr :value login-url))

(defn make-request-handler
  []
  (-> (clout/handle routes/get-course-page
                    (fn [req]
                      {:status 200
                       :headers {"Content-Type" "text/html"}
                       :body (apply str (course-frame (:student req) (get-in req [:redirect-urls :login])))}))
      (wrap-resource "learning/public")
      file-info/wrap-file-info))