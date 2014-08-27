(ns studyflow.learning.entry-quiz
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.entry-quiz.events :as events :refer [entry-quiz-id]]
            [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.course :as course]
            [studyflow.learning.section-test.events :refer [section-test-id]]
            [schema.core :as s]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))


(defrecord EntryQuiz
    [course-id
     student-id
     state
     current-question-index
     correct-answers])

(defcommand DismissNagScreen!
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defcommand Start!
  :course-id m/Id
  :student-id m/Id
  entry-quiz-id)

(defmethod handle-command ::DismissNagScreen!
  [entry-quiz {:keys [student-id course-id]}]
  (if-not entry-quiz
    [:ok [(events/nag-screen-dismissed course-id student-id)]]
    [:rejected {:student-id :already-dismissed-nag-screen}]))

(defmethod handle-event ::events/NagScreenDismissed
  [_ {:keys [student-id course-id]}]
  (->EntryQuiz course-id student-id :nag-screen-dismissed 0 0))

(defmethod handle-command ::Start!
  [entry-quiz {:keys [student-id course-id]}]
  (if (or (not entry-quiz)
          (= :nag-screen-dismissed (:state entry-quiz)))
    [:ok [(events/started course-id student-id)
          (events/instructions-read course-id student-id)]]
    [:rejected {:student-id :already-started}]))

(defmethod handle-event ::events/Started
  [entry-quiz {:keys [student-id course-id]}]
  (if entry-quiz
    (assoc entry-quiz :state :started)
    (->EntryQuiz course-id student-id :started 0 0)))

(defmethod handle-event ::events/InstructionsRead
  [entry-quiz _]
  (assoc entry-quiz :state :instructions-read))

(defcommand SubmitAnswer!
  :course-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :inputs {m/FieldName s/Str}
  entry-quiz-id)

(defmethod aggregate-ids ::SubmitAnswer!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::SubmitAnswer!
  [{:keys [student-id course-id current-question-index] :as entry-quiz}
   {:keys [inputs]}
   {{:keys [threshold questions]} :entry-quiz :as course}]

  (let [question (course/question-for-entry-quiz course current-question-index)
        answered-event (if (course/answer-correct? question inputs)
                         (events/question-answered-correctly course-id student-id (:id question) inputs)
                         (events/question-answered-incorrectly course-id student-id (:id question) inputs))
        {:keys [current-question-index correct-answers] :as entry-quiz} (handle-event entry-quiz answered-event)]
    (if (= current-question-index (count questions))
      (if (<= threshold correct-answers)
        [:ok [answered-event (events/passed course-id student-id)]]
        [:ok [answered-event (events/failed course-id student-id)]])
      [:ok [answered-event]])))


(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [{:keys [current-question-index correct-answers] :as entry-quiz} _]
  (-> entry-quiz
      (update-in [:current-question-index] inc)
      (update-in [:correct-answers] inc)))

(defmethod handle-event ::events/QuestionAnsweredIncorrectly
  [{:keys [current-question-index correct-answers] :as entry-quiz} _]
  (-> entry-quiz
      (update-in [:current-question-index] inc)))

(defmethod handle-event ::events/Passed
  [entry-quiz _]
  (assoc entry-quiz :state :passed))

(defmethod handle-event ::events/Failed
  [entry-quiz _]
  (assoc entry-quiz :state :failed))
