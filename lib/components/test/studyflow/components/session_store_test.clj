(ns studyflow.components.session-store-test
  (:require [clojure.test :refer [deftest is]]
            [studyflow.components.session-store :refer [create-session delete-session! get-user-id get-role]]))

(defn create-session-test [session-store]
  (let [user-id "testuuid"
        role "testrole"
        session-id (create-session session-store user-id role 120)]
    (is session-id)
    (is (not (= session-id (create-session session-store user-id role 120))))
    (is (= user-id (get-user-id session-store session-id)))
    (is (= role (get-role session-store session-id)))))

(defn delete-session-test [session-store]
  (let [user-id "testuuid"
        role "testrole"
        session-id (create-session session-store user-id role 120)]
    (delete-session! session-store session-id)
    (is (nil? (get-user-id session-store session-id)))
    (is (nil? (get-role session-store session-id)))))


