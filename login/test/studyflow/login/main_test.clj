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
    (let [user-id "test-user-id"
          user-role "test-role"
          session-store (temp-session-store)
          resp (handler {:login-user {:uuid user-id, :role user-role} :session-store session-store})
          cookies (:cookies resp)]
      (is cookies)
      (let [session-id (:value (:studyflow_session cookies))]
        (is session-id)
        (is (= user-id (get-user-id session-store session-id)))))))

(deftest wrap-logout-user-test
  (let [handler (wrap-logout-user identity)]
    (is (= {} (handler {})))
    (let [resp (handler {:logout-user true, :cookies {:studyflow_session {:value "test", :max-age 123 }}
                         :session-store (temp-session-store)})]
      (is (:cookies resp))
      (is (= {:studyflow_session {:value "", :max-age -1}} (:cookies resp))))))

(deftest wrap-user-role-test
  (let [handler (wrap-user-role identity)
        user-id "test-user-id-2"
        user-role "test-role-2"
        session-store (temp-session-store)]
    (let [session-id (create-session session-store user-id user-role 123)
          resp (handler {:cookies {"studyflow_session" {:value session-id, :max-age 123}}
                         :session-store session-store})]
      (is (:user-role resp))
      (is (= user-role (:user-role resp))))))

(deftest wrap-redirect-for-role-test
  (let [handler (wrap-redirect-for-role identity)
        user-role "test-role"]
    (testing "without any redirect-for-role into request"
      (is (= {} (handler {}))))
    (testing "with redirect-for-role and no cookie"
      (let [resp (handler {:redirect-for-role user-role, :cookies {}})]
        (is (= (default-redirect-path user-role) ((:headers resp) "Location")))))
    (testing "with redirect-for-role and cookie"
      (let [path "this-path"
            resp (handler {:redirect-for-role user-role, :cookies {"studyflow_redir_to" {:value path}}})]
        (is (= path ((:headers resp) "Location")))))))

(deftest get-session-id-from-cookies-test
  (is (= "something"
         (get-session-id-from-cookies {"studyflow_session" {:value "something"}}))))

(deftest make-uuid-cookie-test
  (let [session-id "test-session-id"]
    (testing "without max-age"
      (let [cookie (:studyflow_session (make-session-cookie session-id))]
        (is (= session-id (:value cookie)))
        (is (= session-max-age (:max-age cookie)))))
    (testing "with max-age"
      (let [cookie (:studyflow_session (make-session-cookie session-id 123))]
        (is (= session-id (:value cookie)))
        (is (= 123 (:max-age cookie)))))))

(deftest clear-uuid-cookie-test
  (let [cookie (:studyflow_session (clear-session-cookie))]
    (is (= "" (:value cookie)))
    (is (= -1 (:max-age cookie)))))
