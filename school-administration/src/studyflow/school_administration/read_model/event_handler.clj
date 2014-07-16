(ns studyflow.school-administration.read-model.event-handler
  (:require [rill.message :as message]
            [studyflow.school-administration.read-model :as m]
            [studyflow.school-administration.student.events :as events]))

(defmulti handle-event
  (fn [model event] (message/type event)))

(defmethod handle-event :default
  [model _]
  model)

(defn load-model
  [events]
  (reduce handle-event nil events))

(defmethod handle-event ::events/Created
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student student-id {:full-name full-name
                                 :id student-id})
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::events/NameChanged
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student-full-name student-id full-name)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::events/CredentialsAdded
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-credentials student-id credentials)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::events/CredentialsChanged
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-credentials student-id credentials)
      (m/set-aggregate-version student-id number)))
