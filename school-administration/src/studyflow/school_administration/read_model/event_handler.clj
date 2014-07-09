(ns studyflow.school-administration.read-model.event-handler
  (:require [rill.message :as message]
            [studyflow.school-administration.read-model :as m]
            [studyflow.school-administration.student.events :as events]))

(defmulti handle-event
  (fn [model event] (message/type event)))

(defn load-model
  [events]
  (reduce handle-event nil events))

(defmethod handle-event ::events/Created
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name
                                   :id student-id}))

(defmethod handle-event ::events/NameChanged
  [model {:keys [student-id full-name]}]
  (m/set-student-full-name model student-id full-name))

(defmethod handle-event ::events/CredentialsAdded
  [model {:keys [student-id credentials]}]
  (m/set-student-credentials model student-id credentials))

(defmethod handle-event ::events/CredentialsChanged
  [model {:keys [student-id credentials]}]
  (m/set-student-credentials model student-id credentials))
