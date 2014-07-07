(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :refer [request]]  
            [studyflow.login.main :refer :all]
            [studyflow.login.prepare-database :as prep-db]))

(defn query-html [data pattern]
  (seq (enlive/select
        (if (string? data)
          (enlive/html-snippet data)
          data)
        pattern)))

(use-fixtures :each (fn [test]
                      (prep-db/clean-table db)
                      (wcar* (taoensso.carmine/flushdb))
                      (prep-db/seed-table db)
                      (test)
                      (prep-db/clean-table db)))

(deftest actions-test
  (testing "get /"

    (testing "not logged in"
      (let [resp (actions  (request :get "/"))]
        (is (= 200 (:status resp)) "status should be OK")
        ;;(is (= "Please sign in" (query-html (:body resp) [:h2.form-signin-heading])))
        (let [form (query-html (:body resp) [:form.form-signin])]
          (is form)
          (is (query-html form [[(enlive/attr= :method "POST" :action "/")]]))
          (is (query-html form [[:input (enlive/attr= :name "password")]]))
          (is (query-html form [[:input (enlive/attr= :name "email")]]))
          (is (query-html form [[(enlive/attr= :type "submit")]])))))

    (testing "logged in"
      (let [resp (actions (assoc (request :get "/")
                            :user-role "test"
                            :cookies {}))]
        (is (= "test" (:redirect-for-role resp))))))

  (testing "post /"

    (testing "without params"
      (let [resp (actions (assoc ( request :post "/") :authenticate (fn [x y] nil )))]
        (is (= 200 (:status resp)))
        (is (query-html (:body resp) [:h2.form-signin-heading]))
        (is (query-html (:body resp) [:form.form-signin]))))

    (testing "not authenticated"
      (let [resp (actions (-> (request :post "/")
                              (assoc :email "something@email.com" 
                                     :password "password"
                                     :authenticate (fn [x y] nil ))))]
        (is (= 200 (:status resp)))
        (is (query-html (:body resp) [:h2.form-signin-heading]))
        (is (query-html (:body resp) [:form.form-signin]))))

    (testing "authenticated"
      (let [resp (actions (-> (request :post "/")
                              (assoc :email "something@email.com" 
                                     :password "password"
                                     :authenticate (fn [x y] "something"))))]
        (is (= 302 (:status resp)))
        (is (= "/" ((:headers resp) "Location")))
        (is (= "something" (:login-user resp))))))

  (testing "post /logout"
    (let [resp (actions (request :post "/logout"))]
      (is (= 302 (:status resp)))
      (is (= "/" ((:headers resp) "Location")))
      (is (= true (:logout-user resp))))))

(deftest wrap-authenticator-test
  (let [handler (wrap-authenticator identity "testdb")]
    (is (fn? (:authenticate (handler {}))))))

(deftest wrap-login-user-test
  (let [handler (wrap-login-user identity)]
    (is (= {} (handler {})))
    (let [uuid "testuuid"
          role "testrole"
          resp (handler {:login-user {:uuid uuid, :role role}})]
      (is (:cookies resp)) 
      (is (= {:studyflow_session {:value uuid, :max-age session-max-age }} (:cookies resp)))
      (is (= role (role-for-uuid uuid))))))

(deftest wrap-logout-user-test
  (let [handler (wrap-logout-user identity)]
    (is (= {} (handler {})))
    (let [resp (handler {:logout-user true, :cookies {:studyflow_session {:value "test", :max-age 123 }}})]
      (is (:cookies resp))
      (is (= {:studyflow_session {:value "", :max-age -1}} (:cookies resp))))
    ))

