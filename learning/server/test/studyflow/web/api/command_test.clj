(ns studyflow.web.api.command-test
  (:require [studyflow.web.api.command :as api]
            [ring.mock.request :refer [request body]]
            [clojure.test :refer [is deftest testing]]
            [clout-link.route :refer [uri-for]]
            [studyflow.web.routes :as routes]
            [rill.uuid :refer [uuid]]
            [rill.message :as message]
            [studyflow.learning.course-material-test :as fixture]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.commands]))

(def input (fixture/read-example-json))
(def parsed-input (material/parse-course-material input))

(deftest web-api
  (testing "command handler"
    (let [cmd (api/handler
               (-> (request :put (uri-for routes/update-course-material (:id input)))
                   (assoc :body input)))]
      (is (= (message/type cmd) :publish-course!)
          "generates a command")
      (is (= (uuid (:id input))
             (:course-id cmd)))
      (is (= (:material cmd) parsed-input)
          "Command has material in correct format"))))
