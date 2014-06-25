(ns studyflow.learning.section-test
  (:require [studyflow.learning.section-test.events :as events]
            [studyflow.learning.section-test.commands :as commands]
            [studyflow.learning.course :as course]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defaggregate SectionTest [section-id
                           current-question-id
                           current-question-answered-correctly?])

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
   (events/question-assigned section-test-id course-id (select-random-question course section-id))])

(defmethod handle-event ::events/Created
  [_ {:keys [section-test-id section-id]}]
  (->SectionTest section-test-id section-id nil false))

(defmethod handle-event ::events/QuestionAssigned
  [this {:keys [question-id]}]
  (assoc this
    :current-question-id question-id
    :current-question-answered-correctly? false))

(defmethod aggregate-ids ::commands/CheckAnswer!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::commands/CheckAnswer!
  [{:keys [current-question-id section-id id] :as this} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id)]}
  (if (course/answer-correct? (course/question-for-section course section-id question-id) inputs)
    [(events/question-answered-correctly id question-id inputs)]
    [(events/question-answered-incorrectly id question-id inputs)]))

(defmethod handle-command ::commands/NextQuestion!
  [section-test command course]
  {:pre [(= (:section-id command) (:section-id section-test))
         (:current-question-answered-correctly? section-test)]}
  [(events/question-assigned (:id section-test) (:id course) (:id (select-random-question course (:section-id command))))])
