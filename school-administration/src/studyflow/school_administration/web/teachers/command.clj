(ns studyflow.school-administration.web.teachers.command
  (:require [clojure.string :as str]
            [compojure.core :refer [POST defroutes]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id uuid]]
            [ring.util.response :refer [redirect-after-post]]
            [studyflow.command-tools :refer [with-claim]]
            [studyflow.school-administration.teacher :as teacher]
            [studyflow.credentials.email-ownership :as email-ownership]
            [studyflow.school-administration.web.command-util :refer :all]))

(defn redirect-to-index []
  (redirect-after-post "/list-teachers"))

(defn redirect-to-edit [id]
  (redirect-after-post (str "/edit-teacher/" id)))

(defn redirect-to-new []
  (redirect-after-post "/new-teacher"))

(defroutes commands
  (POST "/create-teacher"
        {:keys [event-store]
         {:keys [full-name department-id] :as params} :params}
        (let [teacher-id (new-id)]
          (-> event-store
              (try-command (teacher/create! teacher-id (when (not= "" department-id)
                                                         (uuid department-id)) full-name))
              (result->response (merge-flash (redirect-to-edit teacher-id) {:message "teacher created"})
                                (redirect-to-new)
                                params))))

  (POST "/change-teacher-name"
        {:keys [event-store]
         {:keys [teacher-id expected-version full-name] :as params} :params}
        (-> event-store
            (try-command (teacher/change-name! (uuid teacher-id) (Long/parseLong expected-version) full-name))
            (result->response (merge-flash (redirect-to-edit teacher-id) {:message "name updated"})
                              (redirect-to-edit teacher-id)
                              params)))

  (POST "/change-teacher-department"
        {:keys [event-store]
         {:keys [teacher-id expected-version department-id] :as params} :params}
        (let [teacher-id (uuid teacher-id)
              version (Long/parseLong expected-version)
              department-id (if (not= "" department-id) (uuid department-id))]
          (-> event-store
              (try-command (teacher/change-department! teacher-id version department-id))
              (result->response (merge-flash (redirect-to-edit teacher-id) {:message "department updated"})
                                (redirect-to-edit teacher-id)
                                params))))

  (POST "/change-teacher-classes"
        {:keys [event-store]
         {:keys [teacher-id expected-version class-names] :as params} :params}
        (let [teacher-id (uuid teacher-id)
              version (Long/parseLong expected-version)]
          (-> event-store
              (try-command (teacher/change-classes! teacher-id version (set class-names)))
              (result->response (merge-flash (redirect-to-edit teacher-id) {:message "classes updated"})
                                (redirect-to-edit teacher-id)
                                params))))

  (POST "/change-teacher-credentials"
        {:keys [event-store]
         {:keys [teacher-id expected-version email original-email password] :as params} :params}
        (let [id (uuid teacher-id)
              version (Long/parseLong expected-version)
              encrypted-password (if (not= "" (str/trim password)) (bcrypt/encrypt password))
              change-credentials (teacher/change-credentials! id version email encrypted-password)]
          (result->response
           (if (= email original-email)
             (try-command event-store change-credentials)
             (let [[status :as result] (with-claim event-store
                                         (email-ownership/claim! id email)
                                         change-credentials
                                         (email-ownership/release! id email))]
               (when (and (= :ok status) (not= "" original-email))
                 (try-command event-store (email-ownership/release! id original-email)))
               result))
           (merge-flash (redirect-to-edit teacher-id) {:message "credentials updated"})
           (redirect-to-edit teacher-id)
           params))))
