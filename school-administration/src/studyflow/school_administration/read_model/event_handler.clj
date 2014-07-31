(ns studyflow.school-administration.read-model.event-handler
  (:require [rill.message :as message]
            [studyflow.school-administration.read-model :as m]
            [studyflow.school-administration.department.events :as department-events]
            [studyflow.school-administration.school.events :as school-events]
            [studyflow.school-administration.student.events :as student-events]))

(defmulti handle-event
  (fn [model event] (message/type event)))

(defmethod handle-event :default
  [model _]
  model)

(defn load-model
  [events]
  (reduce handle-event nil events))

(defmethod handle-event ::student-events/Created
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student student-id {:id student-id
                                 :full-name full-name})
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/NameChanged
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student-full-name student-id full-name)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/EmailChanged
  [model {:keys [student-id email ::message/number]}]
  (-> model
      (m/set-student-email student-id email)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/CredentialsAdded
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-email student-id (:email credentials))
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/CredentialsChanged
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-email student-id (:email credentials))
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/DepartmentChanged
  [model {:keys [student-id department-id ::message/number]}]
  (-> model
      (m/set-student-department student-id department-id)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student-events/ClassAssigned
  [model {:keys [student-id class-name ::message/number]}]
  (-> model
      (m/set-student-class-name student-id class-name)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::school-events/Created
  [model {:keys [school-id name brin ::message/number]}]
  (-> model
      (m/set-school school-id {:id school-id
                               :name name
                               :brin brin})
      (m/set-aggregate-version school-id number)))

(defmethod handle-event ::school-events/NameChanged
  [model {:keys [school-id name ::message/number]}]
  (-> model
      (m/set-school-name school-id name)
      (m/set-aggregate-version school-id number)))

(defmethod handle-event ::department-events/Created
  [model {:keys [department-id school-id name ::message/number]}]
  (-> model
      (m/set-department department-id {:id department-id
                                       :name name
                                       :school-id school-id})
      (m/set-aggregate-version department-id number)))

(defmethod handle-event ::department-events/NameChanged
  [model {:keys [department-id name ::message/number]}]
  (-> model
      (m/set-department-name department-id name)
      (m/set-aggregate-version department-id number)))

(defmethod handle-event ::department-events/SalesDataChanged
  [model {:keys [department-id licenses-sold status ::message/number]}]
  (-> model
      (m/set-department-sales-data department-id licenses-sold status)
      (m/set-aggregate-version department-id number)))
