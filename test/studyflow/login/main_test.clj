(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :refer [request]]
            [studyflow.login.main :refer :all]))

;; TODO move to common place
(defn query-html [data pattern]
  (seq (enlive/select
        (if (string? data)
          (enlive/html-snippet data)
          data)
        pattern)))

(defn caught-up
  [r]
  (assoc r :credentials {:caught-up true}))

(deftest actions-test
  (testing "get /"

    (testing "not logged in"
      (let [resp (actions (-> (request :get "/")
                              caught-up))]
        (is (= 200 (:status resp)) "status should be OK")
        (let [form (query-html (:body resp) [:form#login_screen])]
          (is form)
          (is (query-html form [[(enlive/attr= :method "POST" :action "/")]]))
          (is (query-html form [[:input (enlive/attr= :name "password")]]))
          (is (query-html form [[:input (enlive/attr= :name "email")]]))
          (is (query-html form [[(enlive/attr= :type "submit")]])))))

    (testing "logged in"
      (let [resp (actions (-> (request :get "/")
                              (assoc :user-role "test"
                                     :cookies {})
                              caught-up))]
        (is (= "test" (:redirect-for-role resp))))))

  (testing "post /"

    (testing "without params"
      (let [resp (actions (-> (assoc (request :post "/")
                                :authenticate-by-email-and-password (fn [x y] nil ))
                              caught-up))]
        (is (= 200 (:status resp)))
        (is (query-html (:body resp) [:h2.login_heading]))
        (is (query-html (:body resp) [:form#login_screen]))))

    (testing "not authenticated"
      (let [resp (actions (-> (request :post "/")
                              (assoc :email "something@email.com"
                                     :password "password"
                                     :authenticate-by-email-and-password (fn [x y] nil ))
                              caught-up))]
        (is (= 200 (:status resp)))
        (is (query-html (:body resp) [:h2.login_heading]))
        (is (query-html (:body resp) [:form#login_screen]))))

    (testing "authenticate-by-email-and-password"
      (let [resp (actions (-> (request :post "/")
                              (assoc :email "something@email.com"
                                     :password "password"
                                     :authenticate-by-email-and-password (fn [x y] "something"))
                              caught-up))]
        (is (= 303 (:status resp)))
        (is (= "/" ((:headers resp) "Location")))
        (is (= "something" (:login-user resp))))))

  (testing "delete /"
    (let [resp (actions (-> (request :delete "/")
                            caught-up))]
      (is (= 303 (:status resp)))
      (is (= "/" ((:headers resp) "Location")))
      (is (= true (:logout-user resp))))))

(deftest wrap-login-user-test
  (let [handler (wrap-login-user identity)]
    (is (= {} (handler {})))
    (let [user-id "test-user-id"
          user-role "test-role"
          resp (handler {:login-user {:user-id user-id, :user-role user-role}})]
      (is (:session resp))
      (is (= user-id (:user-id (:session resp)))))))

(deftest wrap-logout-user-test
  (let [handler (wrap-logout-user identity)]
    (is (= {} (handler {})))
    (let [resp (handler {:logout-user true :session {:foo :bar}})]
      (is (nil? (:session resp))))))

(deftest wrap-redirect-for-role-test
  (let [handler (wrap-redirect-for-role identity)
        user-role "student"
        default-redirect-paths {"student" "this-is-a-url"}]
    (testing "without any redirect-for-role into request"
      (is (= {} (handler {}))))
    (testing "with redirect-for-role and no cookie"
      (let [resp (handler {:redirect-for-role user-role, :cookies {}, :default-redirect-paths default-redirect-paths})]
        (is (= (default-redirect-paths user-role) ((:headers resp) "Location")  "this-is-a-url"))))
    ;; cookie functionality removed
    (testing "with redirect-for-role"
      (let [path "this-is-a-url"
            resp (handler {:redirect-for-role user-role, :default-redirect-paths default-redirect-paths})]
        (is (= path ((:headers resp) "Location")))))))
