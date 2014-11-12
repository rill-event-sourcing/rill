(ns studyflow.learning.section-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course :as course]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord SectionTest
    [section-id
     course-id
     student-id
     current-question-id
     current-question-status
     streak-length
     stumbling-streak
     finished?
     question-finished?
     stuck?
     previous-question-ids])

(defn select-random-question
  [course section-id previous-question-ids]
  (let [question-previously-done? (set previous-question-ids)
        available-questions (filter (fn [question] (not (question-previously-done? (:id question)))) (course/questions-for-section course section-id))]
    (assert (seq available-questions))
    (rand-nth (vec available-questions))))

(defmethod aggregate-ids ::commands/Init!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/Init!
  [section-test {:keys [section-id student-id course-id]} course]
  {:pre [(nil? section-test) student-id section-id course-id course (course/section course section-id)]}
  [:ok [(events/created section-id student-id course-id)
        (events/question-assigned
         section-id
         student-id
         (:id (select-random-question course section-id nil))
         (count (course/questions-for-section course section-id)))]])

(defmethod handle-event ::events/Created
  [_ {:keys [section-id student-id section-id course-id]}]
  (->SectionTest section-id course-id student-id nil nil 0 0 false false false nil))

(defn set-previous-question-ids
  [question-ids question-id question-total]
  (take (Math/floor (* question-total 0.9)) (cons question-id question-ids)))

(defmethod handle-event ::events/QuestionAssigned
  [{:keys [previous-question-ids] :as this} {:keys [question-id question-total]}]
  (assoc this
    :current-question-id question-id
    :current-question-status nil
    :question-finished? nil
    :answer-revealed? nil
    :previous-question-ids (set-previous-question-ids previous-question-ids question-id question-total)))

(defn track-streak-correct
  [{:keys [streak-length stumbling-streak current-question-status] :as this}]
  (if (nil? current-question-status)
    (assoc this
      :current-question-status :answered-correctly
      :stumbling-streak 0
      :streak-length (inc streak-length))
    this))

(defn track-streak-incorrect
  [{:keys [stumbling-streak current-question-status] :as this}]
  (if (nil? current-question-status)
    (assoc this
      :current-question-status :answered-incorrectly
      :stumbling-streak (inc stumbling-streak)
      :streak-length 0)
    (assoc this
      :current-question-status :answered-incorrectly
      :streak-length 0)))

(defmethod handle-event ::events/AnswerRevealed
  [{:keys [current-question-id current-question-status stumbling-streak streak-length] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (-> this
      (assoc :answer-revealed? true)
      (cond->
       (nil? current-question-status)
       (-> (assoc :current-question-status :answer-revealed)
           (assoc :stumbling-streak (inc stumbling-streak))
           (assoc :streak-length 0)))))

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
(def streak-to-stumbling-block 3)

(defn action-will-trigger-stumbling-block?
  [{:keys [stumbling-streak stuck? finished? current-question-status]}]
  (and (not stuck?)
       (not finished?)
       (nil? current-question-status)
       (>= stumbling-streak (dec streak-to-stumbling-block))))

(defn correct-answer-will-unstuck?
  [{:keys [stuck? current-question-status]}]
  (and stuck?
       (nil? current-question-status)))

(defn correct-answer-will-finish-test?
  "Given that the next answer will be correct, check if it should trigger a Finished event"
  [{:keys [streak-length current-question-status finished?]}]
  (and (not finished?)
       (nil? current-question-status)
       (>= streak-length (dec streak-length-to-finish-test))))

(defn correct-answer-will-complete-streak?
  "Given that the next answer will be correct, check if it should trigger a StreakCompleted event"
  [{:keys [finished? streak-length current-question-status]}]
  (and finished?
       (nil? current-question-status)
       (= streak-length (dec streak-length-to-finish-test))))

(defmethod handle-command ::commands/CheckAnswer!
  [{:keys [course-id current-question-id section-id student-id question-finished? finished?] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id)
         course-id
         (not question-finished?)]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    (let [correct-answer-event (events/question-answered-correctly section-id student-id question-id inputs)]
      (if (correct-answer-will-finish-test? this)
        [:ok [correct-answer-event
              (events/finished section-id student-id (:id (course/chapter-for-section course section-id)) course-id)]]
        (if (correct-answer-will-complete-streak? this)
          [:ok [correct-answer-event
                (events/streak-completed section-id student-id)]]
          (if (correct-answer-will-unstuck? this)
            [:ok [correct-answer-event
                  (events/unstuck section-id student-id)]]
            [:ok [correct-answer-event]]))))
    (let [incorrect-answer-event (events/question-answered-incorrectly section-id student-id question-id inputs)]
      (if (action-will-trigger-stumbling-block? this)
        [:ok [incorrect-answer-event
              (events/stuck section-id student-id)]]
        [:ok [incorrect-answer-event]]))))

(defmethod aggregate-ids ::commands/RevealAnswer!
  [{:keys [course-id]}]
  [course-id])


(defmethod handle-command ::commands/RevealAnswer!
  [{:keys [current-question-id section-id student-id question-finished? answer-revealed?] :as this} {:keys [question-id] :as command} course]
  {:pre [(= current-question-id question-id)
         (not answer-revealed?)]}
  (if-let [answer (:worked-out-answer (course/question-for-section course section-id question-id))]
    (if (action-will-trigger-stumbling-block? this)
      [:ok [(events/answer-revealed section-id student-id question-id answer)
            (events/stuck section-id student-id)]]
      [:ok [(events/answer-revealed section-id student-id question-id answer)]])
    [:rejected]))

(defmethod aggregate-ids ::commands/NextQuestion!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/NextQuestion!
  [{:keys [section-id student-id question-finished? previous-question-ids]} command course]
  {:pre [question-finished?]}
  [:ok [(events/question-assigned section-id
                                  student-id
                                  (:id (select-random-question course section-id previous-question-ids))
                                  (count (course/questions-for-section course section-id)))]])

(defmethod handle-event ::events/Finished
  [section-test event]
  (assoc section-test
    :finished? true
    :streak-length 0))

(defmethod handle-event ::events/StreakCompleted
  [section-test event]
  (assoc section-test
    :streak-length 0))

(defmethod handle-event ::events/Stuck
  [section-test event]
  (assoc section-test
    :stuck? true
    :stumbling-streak 0))

(defmethod handle-event ::events/Unstuck
  [section-test event]
  (assoc section-test
    :stuck? false))
