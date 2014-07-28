(ns studyflow.school-administration.web.schools.command
  (:require [compojure.core :refer [POST defroutes]]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id uuid]]
            [ring.util.response :refer [redirect]]
            [studyflow.school-administration.school :as school]
            [studyflow.school-administration.web.command-util :refer :all]))

(defn redirect-to-index []
  (redirect "/list-schools"))

(defn redirect-to-edit [id]
 (redirect (str "/edit-school/" id)))

(defn redirect-to-new []
  (redirect "/new-school"))

(defroutes commands
  (POST "/create-school" {:keys [event-store]
                           {:keys [name brin] :as params} :params}
        (let [school-id (new-id)]
          (-> event-store
              (with-claim
                (school/claim-brin! school-id brin)
                (school/create! school-id name brin)
                (school/release-brin! school-id brin))
              (result->response (redirect-to-index)
                                (redirect-to-new)
                                params))))

  (POST "/change-school-name" {:keys [event-store]
                               {:keys [school-id expected-version name] :as params} :params}
        (-> event-store
            (try-command (school/change-name! (uuid school-id) (Long/parseLong expected-version) name))
            (result->response (redirect-to-index)
                              (redirect-to-edit school-id)
                              params))))
