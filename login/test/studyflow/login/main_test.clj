(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :refer [request]]
            [studyflow.login.main :refer :all]
            [studyflow.login.prepare-database :as prep-db]
            [taoensso.carmine :as car]))  

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

;;; routes

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

;;; wiring

(deftest wrap-authenticator-test
  (let [handler (wrap-authenticator identity "testdb")]
    (is (fn? (:authenticate (handler {}))))))

(deftest wrap-login-user-test
  (let [handler (wrap-login-user identity)]
    (is (= {} (handler {})))
    (let [uuid "testuuid"
          role "testrole"
          resp (handler {:login-user {:uuid uuid, :role role}})
          cookies (:cookies resp)]
      (is cookies)
      (let [session-uuid (:value (:studyflow_session cookies))]
        (is session-uuid)
        (is (= uuid (uuid-from-session session-uuid)))))))

(deftest wrap-logout-user-test
  (let [handler (wrap-logout-user identity)]
    (is (= {} (handler {})))
    (let [resp (handler {:logout-user true, :cookies {:studyflow_session {:value "test", :max-age 123 }}})]
      (is (:cookies resp))
      (is (= {:studyflow_session {:value "", :max-age -1}} (:cookies resp))))))

(deftest wrap-user-role-test
 (let [handler (wrap-user-role identity)
       uuid "testuuid2"
       role "testrole2"]
   (let [session-uuid (create-session uuid role) 
         resp (handler {:cookies {"studyflow_session" {:value session-uuid, :max-age 123}}})]
     (is (:user-role resp))
      (is (= role (:user-role resp))))))

(deftest wrap-redirect-for-role-test
  (let [handler (wrap-redirect-for-role identity)
        role "testrole"]
    (testing "without any redirect-for-role into request"
      (is (= {} (handler {}))))
    (testing "with redirect-for-role and no cookie"
      (let [resp (handler {:redirect-for-role role, :cookies {}})]
        (is (= (default-redirect-path role) ((:headers resp) "Location")))))
    (testing "with redirect-for-role and cookie"
      (let [resp (handler {:redirect-for-role role, :cookies {"studyflow_redir_to" {:value "thispath"}}})]
        (is (= "thispath" ((:headers resp) "Location")))))))

;;;; Redis

(deftest register-uuid!-test
  (let [uuid "testuuid"
        role "testrole"]
    (register-uuid! uuid role)
    (is (= role (wcar* (car/get uuid))))
    (let [ttl (wcar* (car/ttl uuid))]
    (is (and (< (- session-max-age 3) ttl) (>= session-max-age ttl)))   )))

(deftest deregister-uuid!-test
  (let [uuid "testuuid"
        role "testrole"]
    (wcar* (car/set uuid role))
    (is (= role (wcar* (car/get uuid))))
    (deregister-uuid! uuid)
    (is (not (wcar* (car/get uuid))))))

(deftest role-for-uuid-test
  (let [uuid "testuuid"
        role "testrole"]
    (wcar* (car/set uuid role))
    (is (= role (role-for-uuid uuid)))))

(deftest create-session-test
  (let [uuid "testuuid"
        role "testrole"]
    (let [session-uuid (create-session uuid role)
          ttl (wcar* (car/ttl session-uuid))]
      (is session-uuid)
      (is (= uuid (wcar* (car/get session-uuid))))
      (is (and (< (- session-max-age 3) ttl) (>= session-max-age ttl)))
      (is (not (= session-uuid (create-session uuid role)))))))

(deftest delete-session-test
  (let [uuid "testuuid"
        role "testrole"
        session-uuid (create-session uuid role)]
    (delete-session! session-uuid)
    (deregister-uuid! uuid)
    (is (not (wcar* (car/get session-uuid))))
    (is (not (wcar* (car/get uuid))))))

;;;;; Database


;;;;; Cookies

(deftest get-uuid-from-cookies-test
  (is (= "something") (get-uuid-from-cookies {"studyflow_session" {:value "something"}})))

(deftest make-uuid-cookie-test
  (testing "without max-age"
    (let [cookie (:studyflow_session (make-uuid-cookie "testuuid"))]
      (is (= "testuuid" (:value cookie)))
      (is (= session-max-age (:max-age cookie)))))
  (testing "with max-age"
    (let [cookie (:studyflow_session (make-uuid-cookie "testuuid" 123))]
      (is (= "testuuid" (:value cookie)))
      (is (= 123 (:max-age cookie))))))

(deftest clear-uuid-cookie-test
  (let [cookie (:studyflow_session (clear-uuid-cookie))]
    (is (= "" (:value cookie)))
    (is (= -1 (:max-age cookie)))))
