(ns studyflow.school-administration.web.departments.command
  (:require [clojure.string :as str]
            [compojure.core :refer [POST defroutes]]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id uuid]]
            [ring.util.response :refer [redirect]]
            [studyflow.school-administration.department :as department]
            [studyflow.school-administration.import-student :refer [import-tabbed-string]]
            [studyflow.school-administration.web.command-util :refer :all]))

(defn redirect-to-index [school-id]
  (redirect (str "/edit-school/" school-id)))

(defn redirect-to-edit [school-id id]
  (redirect (str "/edit-department/" school-id "/" id)))

(defn redirect-to-new [school-id]
  (redirect (str "/new-department/" school-id)))

(defroutes commands
  (POST "/create-department/:school-id"
        {:keys [event-store]
         {:keys [school-id name] :as params} :params}
        (let [department-id (new-id)
              school-id (uuid school-id)]
          (-> event-store
              (try-command (department/create! department-id school-id name))
              (result->response (redirect-to-index school-id)
                                (redirect-to-new school-id)
                                params))))

  (POST "/change-department-name/:school-id"
        {:keys [event-store]
         {:keys [school-id department-id expected-version name] :as params} :params}
        (let [department-id (uuid department-id)
              version (Long/parseLong expected-version)]
          (-> event-store
              (try-command (department/change-name! department-id version name))
              (result->response (redirect-to-index school-id)
                                (redirect-to-edit school-id department-id)
                                params))))

  (POST "/change-department-sales-data/:school-id"
        {:keys [event-store]
         {:keys [school-id department-id expected-version licenses-sold status] :as params} :params}
        (let [department-id (uuid department-id)
              version (Long/parseLong expected-version)
              licenses-sold (Long/parseLong licenses-sold)]
          (-> event-store
              (try-command (department/change-sales-data! department-id version licenses-sold status))
              (result->response (redirect-to-index school-id)
                                (redirect-to-edit school-id department-id)
                                params))))

  (POST "/import-students/:school-id"
        {:keys [event-store]
         {:keys [department-id student-data school-id]} :params}
        (let [department-id (uuid department-id)
              result (import-tabbed-string event-store department-id student-data)]
          (-> (redirect-to-edit school-id department-id)
              (merge-flash (cond
                            (nil? result)
                            {:warning "Nothing imported.."}

                            (= (:total-imported result) (:total-students result))
                            {:message (format "Successfully imported all %d student(s)."
                                              (:total-students result))}

                            result
                            {:warning (format "Error(s) importing student(s): %d total student(s) in import, %d successfully imported.\n\n%s"
                                              (:total-students result)
                                              (:total-imported result)
                                              (str/join "\n" (map pr-str (:errors result))))}))))))
