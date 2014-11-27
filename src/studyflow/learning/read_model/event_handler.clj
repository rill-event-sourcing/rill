(ns studyflow.learning.read-model.event-handler
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.read-model :as m]
            [studyflow.learning.course.events :as course-events]
            [studyflow.learning.entry-quiz.events :as entry-quiz]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.chapter-quiz.events :as chapter-quiz]
            [studyflow.learning.section-bank.events :as section-bank]
            [rill.event-channel :as event-channel]
            [rill.message :as message]
            [clj-time.coerce :refer [to-local-date]]))

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
      (m/set-student-entry-quiz-status course-id student-id :passed)
      (m/set-remedial-chapters-finished course-id student-id)))

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

(defmethod handle-event ::chapter-quiz/Started
  [model {:keys [chapter-id student-id]}]
  (m/update-student-chapter-quiz-status model chapter-id student-id :started))

(defmethod handle-event ::chapter-quiz/Locked
  [model {:keys [chapter-id student-id]}]
  (m/update-student-chapter-quiz-status model chapter-id student-id :locked))

(defmethod handle-event ::chapter-quiz/UnLocked
  [model {:keys [chapter-id student-id]}]
  (m/update-student-chapter-quiz-status model chapter-id student-id :un-locked))

(defmethod handle-event ::chapter-quiz/Stopped
  [model {:keys [chapter-id student-id]}]
  (m/update-student-chapter-quiz-status model chapter-id student-id :stopped))

(defmethod handle-event ::chapter-quiz/Failed
  [model {:keys [chapter-id student-id]}]
  (m/update-student-chapter-quiz-status model chapter-id student-id :failed))

(defmethod handle-event ::chapter-quiz/Passed
  [model {:keys [chapter-id student-id]}]
  (-> model
      (m/set-chapter-status chapter-id student-id :finished)
      (m/update-student-chapter-quiz-status chapter-id student-id :passed)))

(defmethod handle-event :studyflow.school-administration.teacher.events/Created
  [model {:keys [teacher-id full-name]}]
  (m/set-student model teacher-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.teacher.events/NameChanged
  [model {:keys [teacher-id full-name]}]
  (m/set-student model teacher-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/NameChanged
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [model {:keys [student-id full-name]}]
  (m/set-student model student-id {:full-name full-name}))

;; Coins

(defmethod handle-event ::section-bank/CoinsEarned
  [model {:keys [course-id section-id student-id amount] :as event}]
  (m/add-coins model course-id student-id (to-local-date (message/timestamp event)) amount))


(defmethod handle-event :default
  [model event]
  (log/debug "learning read-model does not handle event" (message/type event))
  model)

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
