(ns studyflow.learning.read-model.event-handler
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.read-model :as m]
            [studyflow.learning.course.events :as course-events]
            [studyflow.learning.entry-quiz.events :as entry-quiz]
            [studyflow.learning.section-test.events :as section-test]
            [rill.event-channel :as event-channel]
            [rill.message :as message]))

(defmulti handle-event
  "Update the read model with the given event"
  (fn [model event] (message/type event)))

(defn update-model
  [model events]
  (reduce handle-event model events))

(defn init-model
  [initial-events]
  (update-model nil initial-events))

(defmethod handle-event ::course-events/Published
  [model event]
  (m/set-course model (:course-id event) (:material event)))

(defmethod handle-event ::course-events/Updated
  [model event]
  (m/set-course model (:course-id event) (:material event)))

(defmethod handle-event ::course-events/Deleted
  [model event]
  (m/remove-course model (:course-id event)))

(defmethod handle-event ::entry-quiz/NagScreenDismissed
  [model {:keys [student-id course-id]}]
  (m/set-student-entry-quiz-status model course-id student-id :nag-screen-dismissed))

(defmethod handle-event ::entry-quiz/Started
  [model {:keys [student-id course-id]}]
  (m/set-student-entry-quiz-status model course-id student-id :started))

(defmethod handle-event ::entry-quiz/Passed
  [model {:keys [student-id course-id]}]
  (-> model
      (m/set-student-remedial-chapters-status course-id student-id :finished)
      (m/set-student-entry-quiz-status course-id student-id :passed)))

(defmethod handle-event ::entry-quiz/Failed
  [model {:keys [student-id course-id]}]
  (m/set-student-entry-quiz-status model course-id student-id :failed))

(defmethod handle-event ::section-test/QuestionAssigned
  [model {:keys [student-id section-id]}]
  (m/update-student-section-status model section-id student-id nil :in-progress))

(defmethod handle-event ::section-test/Stuck
  [model {:keys [student-id section-id]}]
  (m/update-student-section-status model section-id student-id :in-progress :stuck))

(defmethod handle-event ::section-test/Unstuck
  [model {:keys [student-id section-id]}]
  (m/update-student-section-status model section-id student-id :stuck :in-progress))

(defmethod handle-event ::section-test/Finished
  [model {:keys [student-id section-id]}]
  (m/update-student-section-status model section-id student-id :in-progress :finished))

(defmethod handle-event :studyflow.school-administration.teacher.events/Created
  [model {:keys [teacher-id full-name]}]
  (m/set-student model teacher-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name}))

(defmethod handle-event :default
  [model event]
  (log/debug "learning read-model does not handle event" (message/type event))
  model)

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
