(ns studyflow.web.api-test
  (:require [studyflow.web.api :as api]
            [ring.mock.request :refer [request body]]
            [clojure.test :refer [is deftest testing]]
            [clout-link.route :refer [uri-for]]
            [studyflow.web.routes :as routes]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.commands])
  (:import (studyflow.learning.commands UpdateCourse!)))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest web-api
  (testing "command handler"
    (let [cmd (api/command-ring-handler
               (-> (request :put (uri-for routes/update-course-material (:id input)))
                   (assoc :body input)))]
      (is (= (class cmd) UpdateCourse!)
          "generates a command")
      (is (= (:id input)
             (:course-id cmd)))
      (is (= (:material cmd) parsed-input)
          "Command has material in correct format"))))



