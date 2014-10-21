(ns studyflow.learning.web.routes-test
  (:require [clojure.test :refer [deftest is]]
            [studyflow.learning.web.routes :as routes]
            [clout-link.route :refer [uri-for handle]]
            [ring.mock.request :refer [request]]))

(def handler
  (handle routes/update-course-material
          (fn [r]
            :ok)))

(def course-id "1f8e2f2b-ed71-4788-8f08-5529949f2188")

(deftest test-routes-with-uuid
  (is (= :ok
         (handler (request :put (uri-for routes/update-course-material course-id))))))
