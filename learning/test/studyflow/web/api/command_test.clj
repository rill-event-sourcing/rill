(ns studyflow.web.api.command-test
  (:use  [studyflow.web.api.command])
  (:require [ring.mock.request :refer [request body]]
            [clojure.test :refer [is deftest testing]]
            [clojure.tools.logging :as log]
            [clout-link.route :refer [uri-for]]
            [studyflow.web.routes :as routes]
            [rill.temp-store :refer [with-temp-store message=]]
            [rill.uuid :refer [new-id uuid]]
            [rill.message :as message]
            [studyflow.learning.course.fixture :as course-fixture]
            [studyflow.learning.course-material :as course-material]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.section-test.commands :as section-test-commands]
            [studyflow.web.publishing-api :as publishing-api]
            [studyflow.learning.section-test.events :as events]))


;; handle command multi methods
(require '[studyflow.learning.section-test :refer []])

(def student-id (new-id))
(def course-id course-fixture/course-id)
(def course-edn course-fixture/course-edn)
(def section-id (-> course-edn :chapters first :sections first :id))
(def question-id (-> course-edn :chapters first :sections first :questions first :id))

(deftest handler-test
  (testing "init section test"
    (let [req (-> (request :put (uri-for routes/section-test-init
                                         course-id
                                         section-id
                                         student-id))
                  (assoc-in [:student :student-id] student-id))
          cmd (handler req)]
      (is (= ::section-test-commands/Init! (message/type cmd))
          "generates correct command")
      (is (= [section-id course-id]
             (map #(uuid (% cmd)) [:section-id :course-id]))
          "commands has correct ids")

      (with-temp-store [store fetch execute!]
        (is (= :ok (execute! (course-commands/publish! course-id course-edn))))

        (testing "with command executor"
          (let [{:keys [status body]} ((make-request-handler store) req)]
            (is (= 200 status))
            (is (= :command-accepted (:status body)))
            (is (fetch (events/section-test-id {:section-id section-id :student-id student-id}))))))))

  (testing "check section test answer"
    (let [inputs {"name" "value"}
          req (-> (request :put (uri-for routes/section-test-check-answer
                                         section-id
                                         student-id
                                         course-id
                                         question-id))
                  (assoc-in [:student :student-id] student-id)
                  (assoc :body {:expected-version 0
                                :inputs inputs}))
          cmd (handler req)]
      (is (message= (section-test-commands/check-answer! section-id student-id 0 course-id question-id inputs)
                    cmd))

      (with-temp-store [store fetch execute!]
        (is (= :ok (execute! (course-commands/publish! course-id course-edn))))
        (is (= :ok (execute! (section-test-commands/init! section-id student-id course-id))))

        (testing "with command executor"
          (let [section-test (fetch (events/section-test-id {:student-id student-id :section-id section-id}))
                req (-> (request :put (uri-for routes/section-test-check-answer
                                               section-id
                                               student-id
                                               course-id
                                               (:current-question-id section-test)))
                        (assoc-in [:student :student-id] student-id)
                        (assoc :body {:expected-version 1
                                      :inputs inputs}))
                {:keys [status body]} ((make-request-handler store) req)]
            (is (= 200 status))
            (is (= :command-accepted (:status body)))))))))
