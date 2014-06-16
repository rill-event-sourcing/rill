(ns studyflow.web.api-test
  (:require [studyflow.web.api :as api]
            [clojure.test :refer [deftest is testing]]
            [rill.event-store.memory :refer [memory-store]]
            [studyflow.learning.commands :as commands]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.course-material :as material]
            [clout-link.route :refer [uri-for]]
            [studyflow.web.routes :as routes]
            [ring.mock.request :refer [request body content-type]]))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest test-middleware
  (let [h (fn [_] {:data :value})]
    (is (= (h :foo) {:data :value}))
    (is ((api/wrap-middleware h) :foo))))

(deftest test-api-request-handler
  (let [handler (api/make-request-handler (memory-store) (atom {}))
        r (-> (request :put (uri-for routes/update-course-material (:id input)))
              (body (slurp "test/studyflow/material.json"))
              (content-type "application/json"))]
    (is (= (:body (handler r))
           "{\"status\":\"command-accepted\"}"))))
