(ns studyflow.learning.web.authentication-test
  (:require [studyflow.learning.web.authentication :refer :all]
            [clojure.test :refer :all]
            [studyflow.learning.read-model :refer [set-student]]))

(defn redirect?
  [response]
  (#{303 302} (:status response)))

(defn ok?
  [response]
  (= 200 (:status response)))

(deftest test-wrap-authentication
  (let [handler (wrap-authentication (fn [_] {:status 200 :body "Ok"}))
        read-model (set-student nil "123" {:full-name "J. Test"})]
    (is (redirect? (handler {}))
        "needs a session to continue")
    (is (ok? (handler {:session {:user-role "student"
                                 :user-id "123"}
                       :read-model read-model}))
        "can log in as a student")
    (is (ok? (handler {:session {:user-role "teacher"
                                 :user-id "123"}
                       :read-model read-model}))
        "can log in as a teacher")
    (is (redirect? (handler {:session {:user-role "some-other-role"
                                       :user-id "123"}
                             :read-model read-model}))
        "can't log in as something else")

    (testing "Must have a valid user-role and user-id"
      (is (redirect? (handler {:session {}
                               :read-model read-model})))
      (is (redirect? (handler {:session {:user-id "123"}
                               :read-model read-model})))
      (is (redirect? (handler {:session {:user-role "student"}
                               :read-model read-model})))
      (is (redirect? (handler {:session {:user-id "1234"
                                         :user-role "student"}
                               :read-model read-model}))
          "invalid user ids are ignored"))))
