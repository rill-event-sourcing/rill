(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.login.credentials :refer [add-edu-route-credentials
                                                 add-email-and-password-credentials
                                                 authenticate-by-edu-route-id
                                                 authenticate-by-email-and-password
                                                 handle-event
                                                 change-email]]
            [studyflow.school-administration.student.events :as student-events]))

(deftest authenticate-test
  (testing "authentication with email and password"
    (is (nil? (authenticate-by-email-and-password {} "fred@example.com" "wilma")))
    (let [db (add-email-and-password-credentials nil "my-id"
                                                 {:email "fred@example.com"
                                                  :encrypted-password (bcrypt/encrypt "wilma")})]
      (is (= "my-id" (:user-id (authenticate-by-email-and-password db "fred@example.com" "wilma"))))
      (is (nil? (authenticate-by-email-and-password db "other@example.com" "foobar")))))
  (testing "authentication with eduroute token"
    (is (nil? (authenticate-by-edu-route-id {} "12345")))
    (let [db (add-edu-route-credentials nil "my-id" "12345")]
      (is (= "my-id" (:user-id (authenticate-by-edu-route-id db "12345"))))
      (is (nil? (authenticate-by-edu-route-id db "foobar"))))))

(deftest handle-event-test
  (testing "credentials db ignores unknown events"
    (is (= {}
           (handle-event {}
                         {message/type :some-other-event}))))
  (testing "student events"
    (let [encrypted-password (bcrypt/encrypt "token")]
      (is (= {:by-email {"email" {:user-id "id"
                                  :user-role "student"
                                  :encrypted-password encrypted-password}}}
             (handle-event {}
                           (student-events/credentials-added "id"
                                                             {:email "email"
                                                              :encrypted-password encrypted-password})))))
    (testing "credentials-changed"
      (let [token-encrypted-password (bcrypt/encrypt "token")
            newpassword-encrypted-password (bcrypt/encrypt "newpassword")
            db (-> {}
                   (handle-event (student-events/credentials-added "id"
                                                                   {:email "email"
                                                                    :encrypted-password token-encrypted-password}))
                   (handle-event (student-events/credentials-changed "id"
                                                                     {:email "email"
                                                                      :encrypted-password newpassword-encrypted-password})))]
        (is (= "id" (:user-id (authenticate-by-email-and-password db "email" "newpassword")))
            "can log in with changed password")
        (is (not (authenticate-by-email-and-password db "email" "token"))
            "cannot log in with old password")))

    (testing "email changed"
      (let [id "id"
            password "password"
            old-email "old@example.com"
            new-email "new@example.com"
            db (-> {}
                   (handle-event (student-events/credentials-added id {:email old-email :encrypted-password (bcrypt/encrypt password)}))
                   (handle-event (student-events/email-changed id {:email new-email})))]
        (is (= id (:user-id (authenticate-by-email-and-password db new-email password))))
        (is (not (authenticate-by-email-and-password db old-email password))))))

  (testing "edu-route-events"
    (let [db (-> nil
                 (handle-event (student-events/edu-route-credentials-added "my-id" "12345")))]
      (is (= "my-id" (:user-id (authenticate-by-edu-route-id db "12345")))))))
