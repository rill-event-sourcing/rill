(ns studyflow.web.api-test
  (:use [clojure.test])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clout-link.route :refer [uri-for]]
            [rill.event-store.memory :refer [memory-store]]
            [ring.mock.request :refer [body content-type request]]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.web.api :as api]
            [studyflow.web.routes :as routes]))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest test-api-request-handler
  (let [handler (api/make-request-handler (memory-store) (atom {}))
        req (-> (request :put (uri-for routes/update-course-material (:id input)))
                (body (slurp (io/resource "dev/material.json")))
                (content-type "application/json"))
        res (handler req)
        json (json/parse-string (:body res) true)]
    (is res)
    (is json)
    (is (= "command-accepted" (:status json)))))
