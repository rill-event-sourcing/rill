(ns studyflow.learning.chapter-quiz
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.chapter-quiz.events :as events :refer [chapter-quiz-id]]
            [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.course :as course]
            [schema.core :as s]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord ChapterQuiz
    [chapter-id
     student-id
     current-question-id
     current-question-set-index
     previously-seen-questions ;; question-set-id => #{ question-id ... }
     number-of-errors])

(defcommand Start!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::Start!
  [{:keys [course-id]}]
  [course-id])

(defn select-random-question
  [question-set previous-question-ids]
  (let [question-previously-done? (set previous-question-ids)
        available-questions (filter (fn [question] (not (question-previously-done? (:id question))))
                                    (:questions question-set))]
    (assert (seq available-questions))
    (rand-nth (vec available-questions))))

(defmethod handle-command ::Start!
  [{:keys [previously-seen-questions locked? running?] :as chapter-quiz} {:keys [student-id course-id chapter-id]} course]
  (if (or locked? running?)
    [:rejected]
    (let [question-set (first (course/question-sets-for-chapter-quiz course chapter-id))]
      [:ok [(events/started course-id chapter-id student-id)
            (events/question-assigned course-id chapter-id student-id
                                      (select-random-question question-set
                                                              (get previously-seen-questions (:id question-set))))]])))

(defmethod handle-event ::events/Started
  [chapter-quiz {:keys [course-id student-id chapter-id]}]
  (if chapter-quiz
    ;; restarted
    (assoc chapter-quiz
      :number-of-error 0
      :running? true)
    (->ChapterQuiz chapter-id student-id nil 0 {} 0)))

(defcommand SubmitAnswer!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :expected-version s/Int
  :inputs {m/FieldName s/Str}
  chapter-quiz-id)

(defmethod aggregate-ids ::SubmitAnswer!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::SubmitAnswer!
  [{:keys [current-question-id chapter-id student-id] :as chapter-quiz} {:keys [inputs question-id] :as command} course]
  (if (course/answer-correct? (course/question-for-chapter-quiz course chapter-id current-question-id))
    ...
    ...))

(defcommand DismissErrorScreen!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::DismissErrorScreen!
  [{:keys [course-id]}]
  [course-id])

(defcommand Stop!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)
