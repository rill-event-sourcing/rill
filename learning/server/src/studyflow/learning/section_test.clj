(ns studyflow.learning.section-test
  (:require [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course :as course]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defaggregate SectionTest [section-id
                           current-question-id
                           current-question-status
                           streak-length
                           finished?])

(defn select-random-question
  [course section-id]
  (rand-nth (vec (course/questions-for-section course section-id))))

(defmethod aggregate-ids ::commands/Init!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/Init!
  [section-test {:keys [section-test-id section-id course-id]} course]
  {:pre [(nil? section-test) (course/section course section-id)]}
  [(events/created section-test-id course-id section-id)
   (events/question-assigned section-test-id course-id (:id (select-random-question course section-id)))])

(defmethod handle-event ::events/Created
  [_ {:keys [section-test-id section-id]}]
  (->SectionTest section-test-id section-id nil nil 0 false))

(defmethod handle-event ::events/QuestionAssigned
  [this {:keys [question-id]}]
  (assoc this
    :current-question-id question-id
    :current-answer-status nil))

(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [{:keys [current-question-id current-question-status streak-length] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (if (nil? current-question-status)
    (assoc this
      :current-question-status :answered-correctly
      :streak-length (inc streak-length))
    this))

(defmethod handle-event ::events/QuestionAnsweredIncorrectly
  [{:keys [current-question-id] :as this} {:keys [question-id]}]
  {:pre [(= current-question-id question-id)]}
  (assoc this
    :current-question-status :answered-incorrectly
    :streak-length 0))

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
  [{:keys [current-question-id section-id id finished?] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id) (not finished?)]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    (if (correct-answer-will-finish-test? this)
      [(events/question-answered-correctly id question-id inputs)
       (events/finished id)]
      [(events/question-answered-correctly id question-id inputs)])
    [(events/question-answered-incorrectly id question-id inputs)]))

(defmethod aggregate-ids ::commands/NextQuestion!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/NextQuestion!
  [section-test command course]
  {:pre [(= (:section-id command) (:section-id section-test))
         (:current-question-answered-correctly? section-test)]}
  [(events/question-assigned (:id section-test) (:id course) (:id (select-random-question course (:section-id command))))])
