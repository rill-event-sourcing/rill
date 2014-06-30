(ns studyflow.web.api-test
  (:require [clojure.test :refer [deftest is]]
            [clout-link.route :refer [uri-for]]
            [rill.event-store.memory :refer [memory-store]]
            [ring.mock.request :refer [body content-type request]]
            [studyflow.learning.course.commands :as commands]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.web.api :as api]
            [studyflow.web.routes :as routes]))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest test-api-request-handler
  (let [handler (api/make-request-handler (memory-store) (atom {}))
        r (-> (request :put (uri-for routes/update-course-material (:id input)))
              (body (slurp "test/studyflow/material.json"))
              (content-type "application/json"))]
    (is (= (:body (handler r))
           "{\"status\":\"command-accepted\"}"))))
