(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt]
            [rill.message :as message]
            [studyflow.login.credentials :refer [authenticate-by-email-and-password
                                                 handle-event add-email-and-password-credentials]]
            [studyflow.school-administration.student.events :as student-events]))

(deftest authenticate-test
  (is (nil? (authenticate-by-email-and-password {} "fred@example.com" "wilma")))
  (let [db (add-email-and-password-credentials nil "my-id"
                                               {:email "fred@example.com"
                                                :encrypted-password (bcrypt/encrypt "wilma")})]
    (is (= "my-id" (:uuid (authenticate-by-email-and-password db "fred@example.com" "wilma"))))
    (is (nil? (authenticate-by-email-and-password db "other@example.com" "foobar")))))

(deftest handle-event-test
  (is (= {}
         (handle-event {}
                       {message/type :some-other-event})))
  (is (= (-> nil
             (handle-event (student-events/credentials-added "id" {:email "email"
                                                                   :encrypted-password (bcrypt/encrypt "token")}))
             (authenticate-by-email-and-password "email" "token"))))
  (let [db (-> nil
               (handle-event (student-events/credentials-added "id" {:email "email"
                                                                     :encrypted-password (bcrypt/encrypt "token")}))
               (handle-event (student-events/credentials-changed "id" {:email "email"
                                                                       :encrypted-password (bcrypt/encrypt "token2")})))]
    (is (= "id" (:uuid (authenticate-by-email-and-password db "email" "token2")))
        "can log in with changed password")
    (is (not (authenticate-by-email-and-password db "email" "token"))
        "cannot log in with old password")))
