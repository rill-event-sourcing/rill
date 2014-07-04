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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; views

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; models

(deftest actions-test
  (testing "get /"

    (testing "not logged in"
      (let [resp (actions  (request :get "/"))]
        (is (= 200 (:status resp)) "status should be OK")
        (let [form (query-html (:body resp) [[:form.login]])]
          (is form)
          (is (query-html form [[(enlive/attr= :method "POST")]]))
          (is (query-html form [[(enlive/attr= :action "/")]]))
          (is (query-html form [[:input (enlive/attr= :name "password")]]))
          (is (query-html form [[:input (enlive/attr= :name "email")]]))
          (is (query-html form [[(enlive/attr= :type "submit")]])))))

    (testing "logged in"
      (let [resp (actions (assoc (request :get "/")
                            :user-role "test"
                            :cookies {}))]
        (is (= "test" (:redirect-for-role resp)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controllers
