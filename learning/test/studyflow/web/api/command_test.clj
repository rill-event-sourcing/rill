(ns studyflow.web.api.command-test
  (:use  [studyflow.web.api.command])
  (:require [ring.mock.request :refer [request body]]
            [clojure.test :refer [is deftest testing]]
            [clojure.tools.logging :as log]
            [clout-link.route :refer [uri-for]]
            [studyflow.web.routes :as routes]
            [rill.temp-store :refer [with-temp-store]]
            [rill.uuid :refer [new-id uuid]]
            [rill.message :as message]
            [studyflow.learning.course.fixture :as course-fixture]
            [studyflow.learning.course-material :as course-material]
            [studyflow.learning.course.commands :as course-commands]
            [studyflow.learning.section-test.commands :as section-test-commands]
            [studyflow.web.publishing-api :as publishing-api]))

;; handle command multi methods
(require '[studyflow.learning.section-test :refer []])

(def course-id course-fixture/course-id)
(def course-edn course-fixture/course-edn)
(def section-id (-> course-edn :chapters first :sections first :id))
(def question-id (-> course-edn :chapters first :sections first :questions first :id))

(deftest handler-test
  (testing "update course material"
    (let [input course-fixture/course-json
          parsed-input (course-material/parse-course-material input)
          req (-> (request :put (uri-for routes/update-course-material
                                         (:id input)))
                  (assoc :body input))
          cmd (publishing-api/publish-course-handler req)]
        (is (= ::course-commands/Publish! (message/type cmd))
            "generates correct command")
        (is (= (uuid (:id input)) (:course-id cmd))
            "command has correct course id")
        (is (= parsed-input (:material cmd))
            "command has material in correct format")

        (with-temp-store [store fetch _]
          (testing "with command executor"
            (let [{:keys [status body] :as res} ((publishing-api/make-handler store) req)]
              (is (= 200 status))
              (is (= :command-accepted (:status body)))
              (is (fetch course-id)))))))

  (testing "init section test"
    (let [section-test-id (str "student-idSTUDENTMOCKsection-id" (str section-id))
          req (request :put (uri-for routes/section-test-init
                                     course-id
                                     section-id
                                     section-test-id))
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
            (is (fetch section-test-id)))))))

  (testing "check section test answer"
    (let [section-test-id (str "student-idSTUDENTMOCKsection-id" (str section-id))
          inputs {"name" "value"}
          req (-> (request :put (uri-for routes/section-test-check-answer
                                         section-test-id
                                         section-id
                                         course-id
                                         question-id))
                  (assoc :body {:expected-version 0
                                :inputs inputs}))
          cmd (handler req)]
      (is (= ::section-test-commands/CheckAnswer! (message/type cmd))
          "generates correct command")
      (is (= [section-id course-id question-id]
             (map #(uuid (% cmd)) [:section-id :course-id :question-id]))
          "commands has correct ids")
      (is (= section-test-id (get cmd :section-test-id))
          "commands has correct ids")
      (is (= inputs (:inputs cmd))
          "commands has correct inputs")

      (with-temp-store [store fetch execute!]
        (is (= :ok (execute! (course-commands/publish! course-id course-edn))))
        (is (= :ok (execute! (section-test-commands/init! section-test-id section-id course-id))))

        (testing "with command executor"
          (let [section-test (fetch section-test-id)
                req (-> (request :put (uri-for routes/section-test-check-answer
                                               section-test-id
                                               section-id
                                               course-id
                                               (:current-question-id section-test)))
                        (assoc :body {:expected-version 1
                                      :inputs inputs}))
                {:keys [status body]} ((make-request-handler store) req)]
            (is (= 200 status))
            (is (= :command-accepted (:status body)))))))))
