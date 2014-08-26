(ns studyflow.teaching.read-model.event-handler
  (:require [rill.message :as message]
            [rill.event-channel :as event-channel]
            [studyflow.teaching.read-model :as m]))

(defmulti handle-event
  (fn [model event] (message/type event)))

(defmethod handle-event :default
  [model _]
  model)

(defn load-model
  [events]
  (reduce handle-event nil events))

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.student.events/NameChanged
  [model {:keys [student-id full-name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.student.events/DepartmentChanged
  [model {:keys [student-id department-id ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.student.events/ClassAssigned
  [model {:keys [student-id class-name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [model {:keys [student-id full-name department-id class-name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.school.events/Created
  [model {:keys [school-id name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.school.events/NameChanged
  [model {:keys [school-id name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.department.events/Created
  [model {:keys [department-id school-id name ::message/number]}]
  model)

(defmethod handle-event :studyflow.school-administration.department.events/NameChanged
  [model {:keys [department-id name ::message/number]}]
  model)

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
