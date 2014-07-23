(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :refer [request]]
            [studyflow.login.main :refer :all]
            [studyflow.components.temp-session-store :refer [temp-session-store]]
            [studyflow.components.session-store :refer [get-user-id create-session]]
            [taoensso.carmine :as car]))

(defn query-html [data pattern]
  (seq (enlive/select
        (if (string? data)
          (enlive/html-snippet data)
          data)
        pattern)))

(deftest actions-test
  (testing "get /"

    (testing "not logged in"
      (let [resp (actions (request :get "/"))]
        (is (= 200 (:status resp)) "status should be OK")
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

  (testing "delete /"
    (let [resp (actions (request :delete "/"))]
      (is (= 302 (:status resp)))
      (is (= "/" ((:headers resp) "Location")))
      (is (= true (:logout-user resp))))))



(deftest wrap-login-user-test
  (let [handler (wrap-login-user identity)]
    (is (= {} (handler {})))
    (let [uuid "testuuid"
          role "testrole"
          session-store (temp-session-store)
          resp (handler {:login-user {:uuid uuid, :role role} :session-store session-store})
          cookies (:cookies resp)]
      (is cookies)
      (let [session-uuid (:value (:studyflow_session cookies))]
        (is session-uuid)
        (is (= uuid (get-user-id session-store session-uuid)))))))

(deftest wrap-logout-user-test
  (let [handler (wrap-logout-user identity)]
    (is (= {} (handler {})))
    (let [resp (handler {:logout-user true, :cookies {:studyflow_session {:value "test", :max-age 123 }}
                         :session-store (temp-session-store)})]
      (is (:cookies resp))
      (is (= {:studyflow_session {:value "", :max-age -1}} (:cookies resp))))))

(deftest wrap-user-role-test
  (let [handler (wrap-user-role identity)
        uuid "testuuid2"
        role "testrole2"
        session-store (temp-session-store)]
    (let [session-uuid (create-session session-store uuid role 123)
          resp (handler {:cookies {"studyflow_session" {:value session-uuid, :max-age 123}}
                         :session-store session-store})]
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

#_(deftest create-session-test
  (let [uuid "testuuid"
        role "testrole"
        session-uuid (create-session uuid role)
        user-uuid (wcar* (car/get session-uuid))
        ttl-session (wcar* (car/ttl session-uuid))
        ttl-user (wcar* (car/ttl user-uuid))]
    (is (not (= session-uuid (create-session uuid role))))
    (is session-uuid)
    (is user-uuid)
    (is (= uuid user-uuid))
    (is (and (< (- session-max-age 3) ttl-session)
             (>= session-max-age ttl-session)))
    (is (and (< (- session-max-age 3) ttl-user)
             (>= session-max-age ttl-user)))))

#_(deftest delete-session-test
  (let [uuid "testuuid"
        role "testrole"
        session-uuid (create-session uuid role)]
    (delete-session! session-uuid)
    (is (not (wcar* (car/get session-uuid))))
    (is (not (wcar* (car/get uuid))))))

#_(deftest role-from-session-test
  (let [uuid "testuuid"
        role "testrole"
        session-uuid (create-session uuid role)]
    (is (= role (role-from-session session-uuid)))))

(deftest get-uuid-from-cookies-test
  (is (= "something"
         (get-uuid-from-cookies {"studyflow_session" {:value "something"}}))))

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
