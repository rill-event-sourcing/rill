(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [studyflow.login.main :refer :all]))

(defn query-hiccup [data pattern]
  (enlive/select (enlive/html-snippet (hiccup/html data)) pattern))

(defn query-hiccup-content [data pattern]
  (apply str (:content (first (query-hiccup data pattern)))))

(use-fixtures :each (fn [test]
                      (empty-database db)
                      (seed-database db)
                      (test)
                      (empty-database db)
                      ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; views
(deftest home-page
  (let [data (home db 314 ["first-fake-uuid" "second-fake-uuid"])]
    (testing "Home page includes a welcome message."
      (is (= "welcome " (query-hiccup-content data [:h3]))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; models
(deftest test-count-users
  (let [data (count-users db)]
    (testing "counting-users should give a count of the users"
      (is (= 4 data)))))

(deftest test-list-users
  (let [data (list-users db)]
    (testing "list-users should give a list of users"
      (is (= 4 (count data))))))

(deftest test-create-user
  (let [data (create-user db "tester" "tester@test.nl" "secret")]
    (testing "create-user should add a new user to the database"
      (is (= 5 (count-users db)))
      )
    )
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controllers




