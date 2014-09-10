(ns studyflow.teaching.read-model.event-handler
  (:require [rill.event-channel :as event-channel]
            [clojure.tools.logging :as log]
            [rill.message :as message]
            [studyflow.teaching.read-model :as m]))

(defmulti handle-event (fn [model event] (message/type event)))
(defmethod handle-event :default [model _] model)

;; student administration

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name]}]
  (assoc-in model [:students student-id :full-name] full-name))

(defmethod handle-event :studyflow.school-administration.student.events/NameChanged
  [model {:keys [student-id full-name]}]
  (assoc-in model [:students student-id :full-name] full-name))

(defmethod handle-event :studyflow.school-administration.student.events/DepartmentChanged
  [model {:keys [student-id department-id]}]
  (assoc-in model [:students student-id :department-id] department-id))

(defmethod handle-event :studyflow.school-administration.student.events/ClassAssigned
  [model {:keys [student-id class-name] :as event}]
  (assoc-in model [:students student-id :class-name] class-name))

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
  (assoc-in model [:schools school-id :name] name))

(defmethod handle-event :studyflow.school-administration.department.events/Created
  [model {:keys [department-id school-id name]}]
  (assoc-in model [:departments department-id] {:school-id school-id
                                                :name name}))

(defmethod handle-event :studyflow.school-administration.department.events/NameChanged
  [model {:keys [department-id name]}]
  (assoc-in model [:departments department-id :name] name))

;; finishing sections

(defmethod handle-event :studyflow.learning.section-test.events/Finished
  [model {:keys [section-id student-id]}]
  (update-in model [:students student-id :finished-sections] (fnil conj #{}) section-id))

(defmethod handle-event :studyflow.learning.entry-quiz.events/Passed
  [model {:keys [student-id course-id]}]
  (update-in model [:students student-id :course-entry-quiz-passed] (fnil conj #{}) course-id))

;; course material

(defn strip-course-material [material]
  (let [strip-section (fn [section]
                        (dissoc section :questions :subsections :line-input-fields))
        strip-section-list (fn [sections]
                             (vec (map strip-section sections)))
        strip-chapter (fn [chapter]
                        (update-in chapter [:sections] strip-section-list))
        strip-chapter-list (fn [chapters]
                             (vec (map strip-chapter chapters)))
        strip-course (fn [course]
                       (update-in course [:chapters] strip-chapter-list))]
    (-> (strip-course material)
        (dissoc :entry-quiz))))

(defmethod handle-event :studyflow.learning.course.events/Published
  [model {:keys [course-id material]}]
  (assoc-in model [:courses course-id] (strip-course-material material)))

(defmethod handle-event :studyflow.learning.course.events/Updated
  [model {:keys [course-id material]}]
  (assoc-in model [:courses course-id] (strip-course-material material)))

;; teachers

(defmethod handle-event :studyflow.school-administration.teacher.events/Created
  [model {:keys [teacher-id full-name department-id]}]
  (assoc-in model [:teachers teacher-id] {:department-id department-id :full-name full-name}))

(defmethod handle-event :studyflow.school-administration.teacher.events/DepartmentChanged
  [model {:keys [teacher-id department-id]}]
  (-> model
      (assoc-in [:teachers teacher-id :department-id] department-id)
      (update-in [:teachers teacher-id] dissoc :classes)))

(defmethod handle-event :studyflow.school-administration.teacher.events/ClassAssigned
  [model {:keys [teacher-id department-id class-name] :as event}]
  (update-in model [:teachers teacher-id :classes] (fnil conj #{}) {:department-id department-id
                                                                    :class-name class-name}))

(defmethod handle-event :studyflow.school-administration.teacher.events/ClassUnassigned
  [model {:keys [teacher-id department-id class-name]}]
  (update-in model [:teachers teacher-id :classes] disj {:department-id department-id
                                                         :class-name class-name}))

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
