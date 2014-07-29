(ns studyflow.school-administration.web.students.command
  (:require [clojure.string :as str]
            [compojure.core :refer [POST defroutes]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id uuid]]
            [ring.util.response :refer [redirect]]
            [studyflow.school-administration.student :as student]
            [studyflow.school-administration.web.command-util :refer :all]
            [clojure.tools.logging :as log]))

(defn redirect-to-index []
  (redirect "/list-students"))

(defn redirect-to-edit [id]
  (redirect (str "/edit-student/" id)))

(defn redirect-to-new []
  (redirect "/new-student"))

(defroutes commands
  (POST "/create-student" {:keys [event-store]
                           {:keys [full-name] :as params} :params}
        (let [student-id (new-id)]
          (-> event-store
              (try-command (student/create! student-id full-name))
              (result->response (redirect-to-index)
                                (redirect-to-new)
                                params))))

  (POST "/change-student-name" {:keys [event-store]
                                {:keys [student-id expected-version full-name] :as params} :params}
        (-> event-store
            (try-command (student/change-name! (uuid student-id) (Long/parseLong expected-version) full-name))
            (result->response (redirect-to-index)
                              (redirect-to-edit student-id)
                              params)))

  (POST "/change-student-department" {:keys [event-store]
                                      {:keys [student-id expected-version department-id] :as params} :params}
        (let [student-id (uuid student-id)
              version (Long/parseLong expected-version)
              department-id (if (not= "" department-id) (uuid department-id))]
          (-> event-store
              (try-command (student/change-department! student-id version department-id))
              (result->response (redirect-to-index)
                                (redirect-to-edit student-id)
                                params))))

  (POST "/change-student-credentials" {:keys [event-store]
                                       {:keys [student-id expected-version email original-email password] :as params} :params}
        (let [id (uuid student-id)
              version (Long/parseLong expected-version)
              encrypted-password (if (not= "" (str/trim password)) (bcrypt/encrypt password))

              claim (student/claim-email-address! id email)
              change (student/change-credentials! id version email encrypted-password)
              release (student/release-email-address! id original-email)
              revert (student/release-email-address! id email)]
          (let [r (result->response (if (= email original-email)
                                      (try-command event-store change)
                                      (let [[status :as result] (with-claim event-store claim change revert)]
                                        (when (and (= :ok status) (not= "" original-email))
                                          (try-command event-store release))
                                        result))
                                    (redirect-to-index)
                                    (redirect-to-edit student-id)
                                    params)]
            r))))
