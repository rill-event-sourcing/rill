(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [studyflow.login.main :refer :all]
            [studyflow.login.prepare-database :as prep-db]
            [ring.mock.request :refer [request]]))

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
        (is (not (query-html (:body resp) [:p.warning])))
        (let [form (query-html (:body resp) [:form.login])]
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
        (is (query-html (:body resp) [:p.warning]))
        (is (query-html (:body resp) [:form.login]))))

    (testing "not authenticated"
      (let [resp (actions (-> (request :post "/")
                              (assoc :email "something@email.com" 
                                     :password "password"
                                     :authenticate (fn [x y] nil ))))]
        (is (= 200 (:status resp)))
        (is (query-html (:body resp) [:p.warning]))
        (is (query-html (:body resp) [:form.login]))))

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


