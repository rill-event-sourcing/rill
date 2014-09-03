(ns studyflow.learning.web.browser-resources-test
  (:require [clojure.test :refer [deftest is]]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.course-material :as material]
            [clout-link.route :as route]
            [ring.mock.request :refer [body content-type request]]
            [studyflow.learning.web.browser-resources :as browser-resources]
            [studyflow.learning.web.routes :as routes]))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest html-resources-test
  ;; TODO use a test system based on components
  (let [handler browser-resources/course-page-handler
        r (request :get (route/uri-for routes/get-course-page (:id input)))
        res (handler r)]
    (is (= (:status res) 200))
    (is (= (get-in res [:headers "Content-Type"]) "text/html; charset=utf-8"))))
