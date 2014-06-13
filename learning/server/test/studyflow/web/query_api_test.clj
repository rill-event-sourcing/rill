(ns studyflow.web.query-api-test
  (:require [clojure.test :refer [deftest is testing]]
            [studyflow.web.query-api :refer [make-request-handler]]
            [ring.mock.request :refer [request]]
            [studyflow.web.routes :as routes]
            [clout-link.route :refer [uri-for]]))

(deftest test-query-api
  (let [model (atom {:courses {1 {:name "foo"
                                  :id 1}}})
        handler (make-request-handler model)]
    (is (= (handler (request :get (uri-for routes/query-course-material 1)))
           {:name "foo"}))))
