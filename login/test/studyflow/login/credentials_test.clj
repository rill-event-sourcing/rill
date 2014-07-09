(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [rill.message :as message]
            [crypto.password.bcrypt :as bcrypt]
            [studyflow.events.student :as student-events]
            [studyflow.login.credentials :refer :all]))

(deftest authenticate-test
  (is (nil? (authenticate {} "fred@example.com" "wilma")))
  (is (= "Fred Flintstone"
         (:full-name (authenticate {"fred@example.com" {:full-name "Fred Flintstone"
                                                        :encrypted-password (bcrypt/encrypt "wilma")}}
                                   "fred@example.com"
                                   "wilma")))))

(deftest wrap-authenticator-test
  (let [handler (wrap-authenticator identity)]
    (is (fn? (:authenticate (handler {}))))))

(deftest handle-event-test
  (is (= {}
         (handle-event {}
                       {message/type :some-other-event})))
  (is (= {"email" {:uuid "id"
                   :role "student"
                   :encrypted-password "token"}}
         (handle-event {}
                       (student-events/credentials-added "id" "email" "token"))))
  (let [db (-> {}
               (handle-event (student-events/credentials-added "id" "email" "token"))
               (handle-event (student-events/credentials-changed "id" "email" "newpassword")))
        user (get db "email")]
    (is (= "newpassword" (:encrypted-password user)))
    (is (= "id" (:uuid user)))))



