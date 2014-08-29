(ns studyflow.teaching.web.authentication
  (:require [clojure.tools.logging :as log]
            [ring.middleware.cookies :as cookies]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.components.session-store :as session-store]))

(defn redirect-login [req]
  (let [login-url (get-in req [:redirect-urls :login])
        teaching-url (get-in req [:redirect-urls :teaching])]
    {:status 302
     :headers {"Location" login-url}
     :cookies {"studyflow_teaching_redir_to" {:value (str teaching-url (get req :uri))
                                              :path "/"}
               "studyflow_session" {:value ""
                                    :path "/"
                                    :max-age -1}}
     "body" nil}))

(defn wrap-teacher [handler]
  (fn [{:keys [read-model] :as req}]
    (if-let [teacher-id (get req :teacher-id)]
      (if-let [teacher (read-model/get-teacher read-model teacher-id)]
        (handler (assoc req :teacher (assoc teacher :teacher-id teacher-id)))
        (do
          (log/warn "Can't find teacher through session, perhaps re-logging in will work")
          (redirect-login req)))
      req)))

(defn get-teacher-id [session-store session-id]
  (when-let [user-id (session-store/get-user-id session-store session-id)]
    (when (= (session-store/get-role session-store session-id) "teacher")
      user-id)))

(defn wrap-teacher-id [handler session-store]
  (fn [req]
    (if-let [session-id (get req :session-id)]
      (if-let [teacher-id (get-teacher-id session-store session-id)]
        (handler (-> req
                     (assoc :teacher-id teacher-id)
                     (dissoc :session-id)))
        ;; session expired
        (redirect-login req))
      req)))

(defn wrap-check-cookie [handler]
  (fn [req]
    (if-let [session-id (get-in req [:cookies "studyflow_session" :value])]
      (handler (assoc req :session-id session-id))
      (redirect-login req))))

(defn wrap-authentication [handler session-store]
  (-> handler
      wrap-teacher
      (wrap-teacher-id session-store)
      wrap-check-cookie
      cookies/wrap-cookies))
