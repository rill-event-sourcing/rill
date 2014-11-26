(ns studyflow.learning.section-test
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.section-bank]
            [studyflow.learning.course :as course]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.message :as message]
            [rill.uuid :refer [new-id]]))

(defrecord SectionTest
    [section-id
     course-id
     student-id
     current-question-id
     streak-length
     stumbling-streak
     test-view
     question-state
     stuck?
     test-finished?
     previous-question-ids])

(def test-transitions
  {nil              {::events/QuestionAssigned :question}
   :question        {::events/QuestionAnsweredCorrectly :answer-correct
                     ::events/QuestionAnsweredIncorrectly :question
                     ::events/Stuck :stuck-modal}
   :answer-correct  {::events/Finished :finished-modal
                     ::events/StreakCompleted :completed-modal
                     ::events/QuestionAssigned :question}
   :stuck-modal     {::events/ModalDismissed :question}
   :finished-modal  {::events/QuestionAssigned :question}
   :completed-modal {::events/QuestionAssigned :question}})

(defn update-test-view
  [section-test event]
  (update-in section-test [:test-view] #(get-in test-transitions [% (message/type event)] %)))

(def question-transitions
  {nil                 {::events/QuestionAssigned :open}
   :open               {::events/QuestionAnsweredIncorrectly :incorrect
                        ::events/AnswerRevealed :revealed
                        ::events/QuestionAnsweredCorrectly :correct}
   :correct            {::events/QuestionAssigned :open}
   :incorrect          {::events/QuestionAnsweredCorrectly :finished-incorrect}
   :revealed           {::events/QuestionAnsweredCorrectly :finished-revealed}
   :finished-revealed  {::events/QuestionAssigned :open}
   :finished-incorrect {::events/QuestionAssigned :open}})

(defn update-question-state
  [section-test event]
  (update-in section-test [:question-state] #(get-in question-transitions [% (message/type event)] %)))

(defn update-states
  [section-test event]
  (-> section-test
      (update-question-state event)
      (update-test-view event)))

(defn question-finished?
  [{:keys [question-state]}]
  (contains? #{:correct :finished-revealed :finished-incorrect} question-state))

(defn answer-revealed?
  [{:keys [question-state]}]
  (contains? #{:revealed :finished-revealed} question-state))

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
  (->SectionTest section-id course-id student-id nil 0 0 nil nil false false nil))

(defn set-previous-question-ids
  [question-ids question-id question-total]
  (take (Math/floor (* question-total 0.9)) (cons question-id question-ids)))

(defmethod handle-event ::events/QuestionAssigned
  [{:keys [previous-question-ids] :as this} {:keys [question-id question-total] :as event}]
  (-> this
      (assoc :current-question-id question-id
             :previous-question-ids (set-previous-question-ids previous-question-ids question-id question-total))
      (update-states event)))

(defn track-streak-correct
  [{:keys [streak-length stumbling-streak question-state] :as this}]
  (if (= :open question-state)
    (assoc this :stumbling-streak 0
           :streak-length (inc streak-length))
    this))

(defn track-streak-incorrect ;; also called when revealing
  [{:keys [stumbling-streak question-state] :as this}]
  (if (= :open question-state)
    (assoc this
      :stumbling-streak (inc stumbling-streak)
      :streak-length 0)
    (assoc this
      :streak-length 0)))

(defmethod handle-event ::events/AnswerRevealed
  [{:keys [current-question-id question-state stumbling-streak streak-length] :as this} {:keys [question-id] :as event}]
  (-> this
      track-streak-incorrect
      (update-states event)))

(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [{:keys [current-question-id current-question-status streak-length] :as this} {:keys [question-id] :as event}]
  (-> this
      track-streak-correct
      (update-states event)))

(defmethod handle-event ::events/QuestionAnsweredIncorrectly
  [{:keys [current-question-id] :as this} {:keys [question-id] :as event}]
  (-> this
      track-streak-incorrect
      (update-states event)))

(defmethod aggregate-ids ::commands/CheckAnswer!
  [{:keys [course-id]}]
  [course-id])

(def streak-length-to-finish-test 5)
(def streak-to-stumbling-block 3)

(defn action-will-trigger-stumbling-block?
  [{:keys [stumbling-streak stuck? test-finished? question-state]}]
  (and (not stuck?)
       (not test-finished?)
       (= :open question-state)
       (>= stumbling-streak (dec streak-to-stumbling-block))))

(defn correct-answer-will-unstuck?
  [{:keys [stuck? question-state]}]
  (and stuck?
       (= :open question-state)))

(defmethod handle-command ::commands/CheckAnswer!
  [{:keys [course-id current-question-id section-id student-id] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id) course-id (not (question-finished? this))]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    (if (correct-answer-will-unstuck? this)
      [:ok [(events/question-answered-correctly section-id student-id question-id inputs) (events/unstuck section-id student-id)]]
      [:ok [(events/question-answered-correctly section-id student-id question-id inputs)]])
    (let [incorrect-answer-event (events/question-answered-incorrectly section-id student-id question-id inputs)]
      (if (action-will-trigger-stumbling-block? this)
        [:ok [incorrect-answer-event
              (events/stuck section-id student-id)]]
        [:ok [incorrect-answer-event]]))))

(defmethod aggregate-ids ::commands/RevealAnswer!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/RevealAnswer!
  [{:keys [current-question-id section-id student-id] :as this} {:keys [question-id] :as command} course]
  (if-not (answer-revealed? this)
    (if-let [answer (:worked-out-answer (course/question-for-section course section-id question-id))]
      (if (action-will-trigger-stumbling-block? this)
        [:ok [(events/answer-revealed section-id student-id question-id answer)
              (events/stuck section-id student-id)]]
        [:ok [(events/answer-revealed section-id student-id question-id answer)]])
      [:rejected])
    [:rejected]))

(defn finish-test?
  "Will the current test finish just now?"
  [{:keys [streak-length test-finished?] :as this}]
  (and (not test-finished?)
       (>= streak-length streak-length-to-finish-test)))

(defn complete-streak?
  "Check if we should trigger a StreakCompleted event"
  [{:keys [test-finished? streak-length]}]
  (and test-finished?
       (= streak-length streak-length-to-finish-test)))

(defmethod aggregate-ids ::commands/NextQuestion!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/NextQuestion!
  [{:keys [section-id student-id course-id previous-question-ids] :as section-test} command course]
  (if (question-finished? section-test)
    (cond (finish-test? section-test)
          [:ok [(events/finished section-id student-id (:id (course/chapter-for-section course section-id)) course-id)]]
          (complete-streak? section-test)
          [:ok [(events/streak-completed section-id student-id)]]
          :else
          [:ok [(events/question-assigned section-id
                                          student-id
                                          (:id (select-random-question course section-id previous-question-ids))
                                          (count (course/questions-for-section course section-id)))]])
    [:rejected]))

(defmethod handle-event ::events/Finished
  [section-test event]
  (-> section-test
      (assoc :streak-length 0
             :test-finished? true)
      (update-states event)))

(defmethod handle-event ::events/StreakCompleted
  [section-test event]
  (-> section-test
      (assoc :streak-length 0)
      (update-states event)))

(defmethod handle-event ::events/Stuck
  [section-test event]
  (-> section-test
      (update-states event)
      (assoc :stuck? true
             :stumbling-streak 0)))

(defmethod handle-event ::events/Unstuck
  [section-test event]
  (-> section-test
      (update-states event)
      (assoc :stuck? false)))

(defmethod handle-command ::commands/DismissModal!
  [{:keys [test-view section-id student-id] :as section-test} event]
  (if (= :stuck-modal test-view)
    [:ok [(events/modal-dismissed section-id student-id)]]
    [:rejected]))

(defmethod handle-event ::events/ModalDismissed
  [this event]
  (update-states this event))
