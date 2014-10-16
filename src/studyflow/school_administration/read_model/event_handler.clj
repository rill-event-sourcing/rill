(ns studyflow.school-administration.read-model.event-handler
  (:require [rill.message :as message]
            [rill.event-channel :as event-channel]
            [studyflow.school-administration.read-model :as m]
            [studyflow.school-administration.department.events :as department]
            [studyflow.school-administration.school.events :as school]
            [studyflow.school-administration.student.events :as student]
            [studyflow.school-administration.teacher.events :as teacher]))

(defmulti handle-event
  (fn [model event] (message/type event)))

(defmethod handle-event :default
  [model _]
  model)

(defn load-model
  [events]
  (reduce handle-event nil events))

(defmethod handle-event ::student/Created
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student student-id {:id student-id
                                 :full-name full-name})
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/NameChanged
  [model {:keys [student-id full-name ::message/number]}]
  (-> model
      (m/set-student-full-name student-id full-name)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/EmailChanged
  [model {:keys [student-id email ::message/number]}]
  (-> model
      (m/set-student-email student-id email)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/CredentialsAdded
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-email student-id (:email credentials))
      (m/set-aggregate-version student-id number)))

;; don't care about the payload, we only need to update the aggregate version
(defmethod handle-event ::student/EduRouteCredentialsAdded
  [model {:keys [student-id ::message/number]}]
  (-> model
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/CredentialsChanged
  [model {:keys [student-id credentials ::message/number]}]
  (-> model
      (m/set-student-email student-id (:email credentials))
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/DepartmentChanged
  [model {:keys [student-id department-id ::message/number]}]
  (-> model
      (m/set-student-department student-id department-id)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/ClassAssigned
  [model {:keys [student-id class-name ::message/number]}]
  (-> model
      (m/set-student-class-name student-id class-name)
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::student/Imported
  [model {:keys [student-id full-name department-id class-name credentials ::message/number]}]
  (-> model
      (m/set-student student-id {:id student-id
                                 :full-name full-name})
      (m/set-student-department student-id department-id)
      (m/set-student-class-name student-id class-name)
      (m/set-student-email student-id (:email credentials))
      (m/set-aggregate-version student-id number)))

(defmethod handle-event ::school/Created
  [model {:keys [school-id name brin ::message/number]}]
  (-> model
      (m/set-school school-id {:id school-id
                               :name name
                               :brin brin})
      (m/set-aggregate-version school-id number)))

(defmethod handle-event ::school/NameChanged
  [model {:keys [school-id name ::message/number]}]
  (-> model
      (m/set-school-name school-id name)
      (m/set-aggregate-version school-id number)))

(defmethod handle-event ::department/Created
  [model {:keys [department-id school-id name ::message/number]}]
  (-> model
      (m/set-department department-id {:id department-id
                                       :name name
                                       :school-id school-id})
      (m/set-aggregate-version department-id number)))

(defmethod handle-event ::department/NameChanged
  [model {:keys [department-id name ::message/number]}]
  (-> model
      (m/set-department-name department-id name)
      (m/set-aggregate-version department-id number)))

(defmethod handle-event ::department/SalesDataChanged
  [model {:keys [department-id licenses-sold status ::message/number]}]
  (-> model
      (m/set-department-sales-data department-id licenses-sold status)
      (m/set-aggregate-version department-id number)))



(defmethod handle-event ::teacher/Created
  [model {:keys [teacher-id full-name department-id ::message/number]}]
  (-> model
      (m/set-teacher teacher-id {:id teacher-id :full-name full-name :department-id department-id})
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/ClassAssigned
  [model {:keys [teacher-id class-name ::message/number]}]
  (-> model
      (m/assign-teacher-to-class teacher-id class-name)
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/ClassUnassigned
  [model {:keys [teacher-id class-name ::message/number]}]
  (-> model
      (m/unassign-teacher-from-class teacher-id class-name)
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/NameChanged
  [model {:keys [teacher-id full-name ::message/number]}]
  (-> model
      (m/set-teacher-full-name teacher-id full-name)
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/EmailChanged
  [model {:keys [teacher-id email ::message/number]}]
  (-> model
      (m/set-teacher-email teacher-id email)
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/CredentialsAdded
  [model {:keys [teacher-id credentials ::message/number]}]
  (-> model
      (m/set-teacher-email teacher-id (:email credentials))
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/CredentialsChanged
  [model {:keys [teacher-id credentials ::message/number]}]
  (-> model
      (m/set-teacher-email teacher-id (:email credentials))
      (m/set-aggregate-version teacher-id number)))

(defmethod handle-event ::teacher/DepartmentChanged
  [model {:keys [teacher-id department-id ::message/number]}]
  (-> model
      (m/set-teacher-department teacher-id department-id)
      (m/set-aggregate-version teacher-id number)))



;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
