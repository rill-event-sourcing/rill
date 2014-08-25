(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.login.credentials :refer [add-edu-route-credentials
                                                 add-email-and-password-credentials
                                                 change-email-and-password-credentials
                                                 authenticate-by-edu-route-id
                                                 authenticate-by-email-and-password
                                                 handle-event
                                                 change-email]]
            [studyflow.school-administration.student.events :as student-events]))

(deftest authenticate-test
  (testing "authentication with email and password"
    (is (nil? (authenticate-by-email-and-password {} "fred@flintstone.com" "wilma")))
    (let [db (-> nil
                 (add-email-and-password-credentials "1"
                                                     {:email "fred@flintstone.com"
                                                      :encrypted-password (bcrypt/encrypt "wilma")})
                 (add-email-and-password-credentials "2"
                                                     {:email "barney@rubble.com"
                                                      :encrypted-password (bcrypt/encrypt "betty")}))]

      (is (= "1" (:user-id (authenticate-by-email-and-password db "fred@flintstone.com" "wilma"))))
      (is (nil? (authenticate-by-email-and-password db "other@example.com" "foobar")))

      (testing "Change credentials"
        (let [db (change-email-and-password-credentials db "2" {:email "barney@rubble.com" :encrypted-password (bcrypt/encrypt "bamm-bamm")})]
          (is (nil? (authenticate-by-email-and-password db "barney@rubble.com" "betty")))
          (is (= "1" (:user-id (authenticate-by-email-and-password db "fred@flintstone.com" "wilma"))))
          (is (= "2" (:user-id (authenticate-by-email-and-password db "barney@rubble.com" "bamm-bamm"))))))))

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
      (is (= {:email-by-id {"id" "email"}
              :by-email {"email" {:user-id "id"
                                  :user-role "student"
                                  :encrypted-password encrypted-password}}}
             (handle-event {}
                           (student-events/credentials-added "id"
                                                             {:email "email"
                                                              :encrypted-password encrypted-password})))))
    (testing "credentials-changed"
      (let [db (-> {}
                   (handle-event (student-events/credentials-added "1"
                                                                   {:email "fred@flintstone.com"
                                                                    :encrypted-password (bcrypt/encrypt "wilma")}))
                   (handle-event (student-events/credentials-added "2"
                                                                   {:email "barney@rubble.com"
                                                                    :encrypted-password (bcrypt/encrypt "betty")}))
                   (handle-event (student-events/credentials-changed "1"
                                                                     {:email "fred2@flintstone.com"
                                                                      :encrypted-password (bcrypt/encrypt "dino")})))]
        (is (= "1" (:user-id (authenticate-by-email-and-password db "fred2@flintstone.com" "dino")))
            "can log in with changed password")
        (is (= "2" (:user-id (authenticate-by-email-and-password db "barney@rubble.com" "betty")))
            "does not impact credentials of other users")
        (is (not (authenticate-by-email-and-password db "fred@flintstone.com" "wilma"))
            "cannot log in with old password")))

    (testing "email changed"
      (let [db (-> {}
                   (handle-event (student-events/credentials-added "1"
                                                                   {:email "fred@flintstone.com"
                                                                    :encrypted-password (bcrypt/encrypt "wilma")}))
                   (handle-event (student-events/credentials-added "2"
                                                                   {:email "barney@rubble.com"
                                                                    :encrypted-password (bcrypt/encrypt "betty")}))
                   (handle-event (student-events/email-changed "1" {:email "pebble@flintstone.com"})))]
        (is (= "1" (:user-id (authenticate-by-email-and-password db "pebble@flintstone.com" "wilma"))))
        (is (= "2" (:user-id (authenticate-by-email-and-password db "barney@rubble.com" "betty"))))
        (is (not (authenticate-by-email-and-password db "fred@flintstone.com" "wilma"))))))

  (testing "edu-route-events"
    (let [db (-> nil
                 (handle-event (student-events/edu-route-credentials-added "my-id" "12345")))]
      (is (= "my-id" (:user-id (authenticate-by-edu-route-id db "12345")))))))
