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
     current-question-set-id
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
  [question-set previously-seen-questions]
  (let [question-previously-done? (set (get previously-seen-questions (:id question-set)))
        _ (clojure.pprint/pprint question-set)
        available-questions (filter (fn [question] (not (question-previously-done? (:id question))))
                                    (:questions question-set))]
    (assert (seq available-questions))
    (rand-nth (vec available-questions))))

(defmethod handle-command ::Start!
  [{:keys [locked? running? previously-seen-questions] :as chapter-quiz} {:keys [student-id course-id chapter-id]} course]
  (if (or locked? running?)
    [:rejected]
    (let [question-set (first (course/question-sets-for-chapter-quiz course chapter-id))
          _ (clojure.pprint/pprint [:qs question-set])]
      [:ok [(events/started course-id chapter-id student-id)
            (events/question-assigned course-id chapter-id student-id (select-random-question question-set previously-seen-questions))]])))

(defmethod handle-event ::events/Started
  [chapter-quiz {:keys [course-id student-id chapter-id]}]
  (if chapter-quiz
    ;; restarted
    (assoc chapter-quiz
      :number-of-errors 0
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
  [{:keys [current-question-id current-question-set-index course-id chapter-id number-of-errors student-id fast-route? previously-seen-questions] :as chapter-quiz} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id)]}
  (if (course/answer-correct? (course/question-for-chapter-quiz course chapter-id current-question-id) inputs)
    (let [correct-answer-event (events/question-answered-correctly chapter-id student-id question-id inputs)]
      (if (= current-question-set-index
             (count (course/question-sets-for-chapter-quiz course chapter-id)))
        [:ok [correct-answer-event
              (events/passed course-id chapter-id student-id)]]
        (let [next-question-set (get (course/question-sets-for-chapter-quiz course chapter-id) (inc current-question-set-index))]
          [:ok [correct-answer-event
                (events/question-assigned course-id chapter-id student-id (select-random-question next-question-set previously-seen-questions))]])))
    (let [incorrect-answer-event (events/question-answered-correctly chapter-id student-id question-id inputs)]
      (cond

       (and fast-route?
            (= 1 number-of-errors))
       [:ok [incorrect-answer-event
             (events/failed course-id chapter-id student-id)
             (events/locked course-id chapter-id student-id)]]

       (= 2 (number-of-errors))
       [:ok [incorrect-answer-event
             (events/failed course-id chapter-id student-id)]]

       (= current-question-set-index
          (count (course/question-sets-for-chapter-quiz course chapter-id)))
       [:ok [incorrect-answer-event
             (events/passed course-id chapter-id student-id)]]

       :else [:ok [incorrect-answer-event]]))))

(defmethod handle-event ::events/QuestionAssigned
  [{:keys [current-question-set-id current-question-id question-set-id] :as chapter-quiz} _]
  (-> chapter-quiz
      (assoc-in [:previously-seen-questions question-set-id] current-question-id))
  (update-in [:current-question-set-index] inc))

#_(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [{:keys [current-question-set-index current-question-id] :as chapter-quiz} _])

(defmethod handle-event ::events/QuestionAnsweredIncorrectly
  [{:keys [current-question-set-index] :as chapter-quiz} _]
  (update-in chapter-quiz [:number-of-errors] inc))

(defmethod handle-event ::events/Passed
  [chapter-quiz _]
  (assoc chapter-quiz :running? false))

(defmethod handle-event ::events/Failed
  [chapter-quiz _]
  (assoc chapter-quiz :running? false))

(defcommand DismissErrorScreen!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::DismissErrorScreen!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::DismissErrorScreen!
  [{:keys [previously-seen-questions]} {:keys [current-question-set-index chapter-id course-id student-id]} course]
  [:ok (events/question-assigned course-id
                                 chapter-id
                                 student-id
                                 (select-random-question (get (course/question-sets-for-chapter-quiz course chapter-id) (inc current-question-set-index)
)
                                                         previously-seen-questions))])

(defcommand Stop!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod handle-command ::Stop!
  [{:keys [fast-route?]} {:keys [student-id course-id chapter-id]} course]
  (if fast-route?
    [:ok [(events/stopped course-id chapter-id student-id)
          (events/locked course-id chapter-id student-id)]]
    [:ok [(events/stopped course-id chapter-id student-id)]]))
