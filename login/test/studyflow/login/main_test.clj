(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [studyflow.login.main :refer :all]
            [studyflow.login.prepare-database :as prep-db]))

(defn query-hiccup [data pattern]
  (enlive/select (enlive/html-snippet (hiccup/html data)) pattern))

(defn query-hiccup-content [data pattern]
  (apply str (:content (first (query-hiccup data pattern)))))

(use-fixtures :each (fn [test]
                      (prep-db/clean-table db)
                      (wcar* (taoensso.carmine/flushdb))
                      (prep-db/seed-table db)
                      (test)
                      (prep-db/clean-table db)
                      ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; views

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; models
(deftest test-count-users
  (let [data (count-users db)]
    (testing "counting-users should give a count of the users"
      (is (= 4 data)))))

(deftest test-logged-in-users
  (let [user (find-user-by-email db "editor@studyflow.nl")] 
    (set-session! (:uuid user) (:role user))) 
  (let [data (logged-in-users)]
    (testing "logged-in-users-users should give a list of logged in users"
      (is (= 1 (count data))))))

(deftest test-create-user
  (let [data (create-user db "tester" "tester@test.nl" "secret")]
    (testing "create-user should add a new user to the database"
      (is (= 5 (count-users db)))
      )
    )
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controllers




