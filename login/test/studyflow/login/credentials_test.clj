(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.login.credentials :refer [add-edu-route-credentials
                                                 add-email-and-password-credentials
                                                 authenticate-by-edu-route-id
                                                 authenticate-by-email-and-password
                                                 handle-event]]
            [studyflow.school-administration.student.events :as student]))

(deftest authenticate-test
  (testing "authentication with email and password"
    (is (nil? (authenticate-by-email-and-password {} "fred@example.com" "wilma")))
    (let [db (add-email-and-password-credentials nil "my-id"
                                                 {:email "fred@example.com"
                                                  :encrypted-password (bcrypt/encrypt "wilma")})]
      (is (= "my-id" (:uuid (authenticate-by-email-and-password db "fred@example.com" "wilma"))))
      (is (nil? (authenticate-by-email-and-password db "other@example.com" "foobar")))))
  (testing "authentication with eduroute token"
    (is (nil? (authenticate-by-edu-route-id {} "12345")))
    (let [db (add-edu-route-credentials nil "my-id" "12345")]
      (is (= "my-id" (:uuid (authenticate-by-edu-route-id db "12345"))))
      (is (nil? (authenticate-by-edu-route-id db "foobar"))))))

(deftest handle-event-test
  (testing "credentials db ignores unknown events"
    (is (= {}
           (handle-event {}
                         {message/type :some-other-event}))))
  (testing "student events"
    (is (= (-> nil
               (handle-event (student/credentials-added "id" {:email "email"
                                                              :encrypted-password (bcrypt/encrypt "token")}))
               (authenticate-by-email-and-password "email" "token"))))
    (let [db (-> nil
                 (handle-event (student/credentials-added "id" {:email "email"
                                                                :encrypted-password (bcrypt/encrypt "token")}))
                 (handle-event (student/credentials-changed "id" {:email "email"
                                                                  :encrypted-password (bcrypt/encrypt "token2")})))]
      (is (= "id" (:uuid (authenticate-by-email-and-password db "email" "token2")))
          "can log in with changed password")
      (is (not (authenticate-by-email-and-password db "email" "token"))
          "cannot log in with old password")))

  (testing "edu-route-events"
    (let [db (-> nil
                 (handle-event (student/edu-route-credentials-added "my-id" "12345")))]
      (is (= "my-id" (:uuid (authenticate-by-edu-route-id db "12345")))))))
