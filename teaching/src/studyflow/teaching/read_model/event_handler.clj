(ns studyflow.teaching.read-model.event-handler
  (:require [rill.event-channel :as event-channel]
            [rill.message :as message]
            [studyflow.teaching.read-model :as m]))

(defmulti handle-event (fn [model event] (message/type event)))
(defmethod handle-event :default [model _] model)

;; student administration

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name]}]
  (assoc-in model [:students student-id] {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/NameChanged
  [model {:keys [student-id full-name]}]
  (update-in model [:students student-id] assoc :full-name full-name))

(defmethod handle-event :studyflow.school-administration.student.events/DepartmentChanged
  [model {:keys [student-id department-id]}]
  (update-in model [:students student-id] assoc :department-id department-id))

(defmethod handle-event :studyflow.school-administration.student.events/ClassAssigned
  [model {:keys [student-id class-name] :as event}]
  (update-in model [:students student-id] assoc :class-name class-name))

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [model {:keys [student-id full-name department-id class-name]}]
  (assoc-in model [:students student-id] {:full-name full-name
                                          :department-id department-id
                                          :class-name class-name}))

(defmethod handle-event :studyflow.school-administration.school.events/Created
  [model {:keys [school-id name]}]
  (assoc-in model [:schools school-id] {:name name}))

(defmethod handle-event :studyflow.school-administration.school.events/NameChanged
  [model {:keys [school-id name]}]
  (update-in model [:schools school-id] assoc :name name))

(defmethod handle-event :studyflow.school-administration.department.events/Created
  [model {:keys [department-id school-id name]}]
  (assoc-in model [:departments department-id] {:school-id school-id
                                                :name name}))

(defmethod handle-event :studyflow.school-administration.department.events/NameChanged
  [model {:keys [department-id name]}]
  (update-in model [:departments department-id] assoc :name name))

;; section tests

(defmethod handle-event :studyflow.learning.section-test.events/Finished
  [model {:keys [section-id student-id]}]
  (update-in model [:students student-id :finished-sections] (fnil conj #{}) section-id))

;; course material

(defmethod handle-event :studyflow.learning.course.events/Published
  [model {:keys [course-id material]}]
  (assoc-in model [:courses course-id] material))

(defmethod handle-event :studyflow.learning.course.events/Updated
  [model {:keys [course-id material]}]
  (assoc-in model [:courses course-id] material))

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
