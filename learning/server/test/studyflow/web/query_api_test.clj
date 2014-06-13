(ns studyflow.web.query-api-test
  (:require [clojure.test :refer [deftest is testing]]
            [studyflow.web.query-api :refer [make-request-handler wrap-read-model]]
            [studyflow.learning.read-model :as m]
            [studyflow.learning.course-material-test :refer [read-example-json]]
            [studyflow.learning.course-material :as material]
            [ring.mock.request :refer [request]]
            [studyflow.web.routes :as routes]
            [clout-link.route :refer [uri-for]]))

(def material (material/parse-course-material (read-example-json)))
(def course-id (:id material))
(def model (m/set-course m/empty-model course-id material))

(deftest test-query-api
  (let [handler (make-request-handler (atom model))]
    (is (= (:id (handler (request :get (uri-for routes/query-course-material (str course-id)))))
           course-id))))

(deftest test-wrap-read-model
  (let [read-model {:read :model}
        h (fn [r] (:read-model r))
        wrapped (wrap-read-model h (atom read-model))]
    (is (= read-model (wrapped {})))))

