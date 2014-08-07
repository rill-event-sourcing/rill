(ns studyflow.learning.section-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.section-test.events :as events]
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
  [:ok [(events/created section-id student-id course-id)
        (events/question-assigned section-id student-id (:id (select-random-question course section-id)))]])

(defmethod handle-event ::events/Created
  [_ {:keys [section-id student-id section-id]}]
  (->SectionTest section-id student-id nil nil 0 false false))

(defmethod handle-event ::events/QuestionAssigned
  [this {:keys [question-id]}]
  (assoc this
    :current-question-id question-id
    :current-question-status nil
    :question-finished? nil
    :answer-revealed? nil))

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

(defmethod handle-event ::events/AnswerRevealed
  [{:keys [current-question-id current-question-status streak-length] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (-> this
      (assoc :answer-revealed? true)
      (cond->
       (nil? current-question-status)
       (assoc :current-question-status :answer-revealed))
      (assoc :streak-length 0)))

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
  [{:keys [streak-length current-question-status finished?]}]
  (and (not finished?)
       (nil? current-question-status)
       (= streak-length (dec streak-length-to-finish-test))))

(defn correct-answer-will-complete-streak?
  "Given that the next answer will be correct, check if it should trigger a StreakCompleted event"
  [{:keys [finished? streak-length current-question-status]}]
  (and finished?
       (nil? current-question-status)
       (= streak-length (dec streak-length-to-finish-test))))

(defmethod handle-command ::commands/CheckAnswer!
  [{:keys [current-question-id section-id student-id question-finished? finished?] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id)
         (not question-finished?)]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    (if (correct-answer-will-finish-test? this)
      [:ok [(events/question-answered-correctly section-id student-id question-id inputs)
            (events/finished section-id student-id)]]
      (if (correct-answer-will-complete-streak? this)
        [:ok [(events/question-answered-correctly section-id student-id question-id inputs)
              (events/streak-completed section-id student-id)]]
        [:ok [(events/question-answered-correctly section-id student-id question-id inputs)]]))
    [:ok [(events/question-answered-incorrectly section-id student-id question-id inputs)]]))

(defmethod aggregate-ids ::commands/RevealAnswer!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/RevealAnswer!
  [{:keys [current-question-id section-id student-id question-finished? answer-revealed?] :as this} {:keys [question-id] :as command} course]
  {:pre [(= current-question-id question-id)
         (not question-finished?)
         (not answer-revealed?)]}
  (let [answer (:worked-out-answer (course/question-for-section course section-id question-id))]
    [:ok [(events/answer-revealed section-id student-id question-id answer)]]))

(defmethod aggregate-ids ::commands/NextQuestion!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/NextQuestion!
  [{:keys [section-id student-id question-finished?]} command course]
  {:pre [question-finished?]}
  [:ok [(events/question-assigned section-id student-id (:id (select-random-question course (:section-id command))))]])

(defmethod handle-event ::events/Finished
  [section-test event]
  (assoc section-test
    :finished? true
    :streak-length 0))

(defmethod handle-event ::events/StreakCompleted
  [section-test event]
  (assoc section-test
    :streak-length 0))
