(ns studyflow.learning.section-test
  (:require [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course :as course]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord SectionTest
    [section-id
     student-id
     current-question-id
     current-question-status
     streak-length
     finished?
     question-finished?])

(defn select-random-question
  [course section-id]
  (rand-nth (vec (course/questions-for-section course section-id))))

(defmethod aggregate-ids ::commands/Init!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/Init!
  [section-test {:keys [section-id student-id course-id]} course]
  {:pre [(nil? section-test) student-id section-id course-id course (course/section course section-id)]}
  [(events/created section-id student-id course-id)
   (events/question-assigned section-id student-id (:id (select-random-question course section-id)))])

(defmethod handle-event ::events/Created
  [_ {:keys [section-id student-id section-id]}]
  (->SectionTest section-id student-id nil nil 0 false false))

(defmethod handle-event ::events/QuestionAssigned
  [this {:keys [question-id]}]
  (assoc this
    :current-question-id question-id
    :current-question-status nil))

(defn track-streak-correct
  [{:keys [streak-length current-question-status] :as this}]
  (if (nil? current-question-status)
    (assoc this
      :current-question-status :answered-correctly
      :streak-length (inc streak-length))
    this))

(defn track-streak-incorrect
  [this]
  (assoc this
    :current-question-status :answered-incorrectly
    :streak-length 0))

(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [{:keys [current-question-id current-question-status streak-length] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (-> this
      (assoc :question-finished? true)
      track-streak-correct))

(defmethod handle-event ::events/QuestionAnsweredIncorrectly
  [{:keys [current-question-id] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (-> this
      (assoc :question-finished? false)
      track-streak-incorrect))

(defmethod aggregate-ids ::commands/CheckAnswer!
  [{:keys [course-id]}]
  [course-id])

(def streak-length-to-finish-test 5)

(defn correct-answer-will-finish-test?
  "Given that the next answer will be correct, check if it should trigger a Finished event"
  [{:keys [streak-length current-question-status]}]
  (and (nil? current-question-status)
       (= streak-length (dec streak-length-to-finish-test))))

(defmethod handle-command ::commands/CheckAnswer!
  [{:keys [current-question-id section-id student-id finished?] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id) (not finished?)]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    (if (correct-answer-will-finish-test? this)
      [(events/question-answered-correctly section-id student-id question-id inputs)
       (events/finished section-id student-id)]
      [(events/question-answered-correctly section-id student-id question-id inputs)])
    [(events/question-answered-incorrectly section-id student-id question-id inputs)]))

(defmethod aggregate-ids ::commands/NextQuestion!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/NextQuestion!
  [{:keys [section-id student-id question-finished?]} command course]
  {:pre [question-finished?]}
  [(events/question-assigned section-id student-id (:id (select-random-question course (:section-id command))))])

(defmethod handle-event ::events/Finished
  [section-test event]
  (assoc section-test :finished? true))
