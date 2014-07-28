(ns studyflow.web.browser-resources
  (:require [clout-link.route :as clout]
            [net.cgrand.enlive-html :as html]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :as file-info]
            [studyflow.web.routes :as routes]))

(html/deftemplate course-frame "templates/courses.html"
  [student]
  [:span.student-full-name] (html/content (get student :full-name))
  [:input#student-id] (html/set-attr :value (str (get student :student-id))))

(defn make-request-handler
  []
  (-> (clout/handle routes/get-course-page
                    (fn [req]
                      {:status 200
                       :headers {"Content-Type" "text/html"}
                       :body (apply str (course-frame (get req :student)))}))
      (wrap-resource "/")
      file-info/wrap-file-info))
