(ns studyflow.web.browser-resources
  (:require [clout-link.route :as clout]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [studyflow.learning.read-model :refer [get-course-id]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [charset]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [studyflow.web.routes :as routes]))

(html/deftemplate course-frame "learning/templates/courses.html"
  [course-id student login-url]
  [:input#course-id] (html/set-attr :value course-id)
  [:input#student-full-name] (html/set-attr :value (:full-name student))
  [:input#student-id] (html/set-attr :value (str (:student-id student)))
  [:input#logout-target] (html/set-attr :value login-url))

(defn wrap-utf-8
  [handler]
  (fn [request]
    (-> request
        handler
        (charset "utf-8"))))

(defn make-request-handler
  []
  (-> (clout/handle routes/get-course-page
                    (fn [{:keys [read-model] {:keys [course-name]} :params :as req}]
                      {:status 200
                       :headers {"Content-Type" "text/html"}
                       :body (apply str (course-frame (get-course-id read-model course-name) (:student req) (get-in req [:redirect-urls :login])))}))
      (wrap-resource "learning/public")
      wrap-content-type
      wrap-utf-8))

