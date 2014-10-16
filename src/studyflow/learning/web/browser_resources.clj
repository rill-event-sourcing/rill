(ns studyflow.learning.web.browser-resources
  (:require [clout-link.route :as clout]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [studyflow.learning.read-model :refer [get-course-id]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response content-type charset]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [studyflow.web.caching :refer [wrap-no-caching]]
            [studyflow.learning.web.routes :as routes]))

(html/deftemplate course-frame "learning/templates/courses.html"
  [course-id student login-url teaching-url]
  [:input#course-id] (html/set-attr :value course-id)
  [:input#student-full-name] (html/set-attr :value (:full-name student))
  [:input#student-id] (html/set-attr :value (str (:student-id student)))
  [:input#logout-target] (html/set-attr :value login-url)
  [:input#teaching-target] (html/set-attr :value teaching-url))

(def course-page-handler
  (clout/handle routes/get-course-page
                (fn [{:keys [read-model student redirect-urls user-role] {:keys [course-name]} :params :as req}]
                  (let [course-id (get-course-id read-model course-name)
                        body (apply str (course-frame course-id student (:login redirect-urls)
                                                      (when (= user-role :teacher)
                                                        (:teacher redirect-urls))))]
                    (-> (response body)
                        (content-type "text/html")
                        (charset "utf-8"))))))

(defn wrap-utf-8
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (-> response (charset "utf-8")))))

(def resource-handler
  (-> (constantly nil)
      (wrap-resource "learning/public")
      wrap-no-caching
      wrap-content-type
      wrap-utf-8))
