(ns studyflow.login.credentials-test
  (:require [clojure.test :refer :all]
            [studyflow.login.credentials :refer :all]))

(deftest wrap-authenticator-test
  (let [handler (wrap-authenticator identity "testdb")]
    (is (fn? (:authenticate (handler {}))))))
