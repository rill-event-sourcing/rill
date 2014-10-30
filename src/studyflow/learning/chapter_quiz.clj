(ns studyflow.learning.chapter-quiz
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [studyflow.learning.chapter-quiz.events :as events :refer [chapter-quiz-id]]
            [studyflow.learning.section-test.events :as section-test]
            [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.course :as course]
            [studyflow.rand :refer [*rand-nth*]]
            [schema.core :as s]
            [rill.aggregate :refer [handle-event handle-command handle-notification aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord ChapterQuiz
    [course-id
     chapter-id
     student-id
     current-question-id
     current-question-set-index
     current-question-set-id
     fast-route?
     previously-seen-questions ;; {question-set-id => #{ question-id ... }}
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
        available-questions (filter (fn [question] (not (question-previously-done? (:id question))))
                                    (:questions question-set))]
    (assert (seq available-questions))
    (*rand-nth* (vec available-questions))))

(defmethod handle-command ::Start!
  [{:keys [locked? running? previously-seen-questions fast-route?] :as chapter-quiz} {:keys [student-id course-id chapter-id]} course]
  (if (or locked? running?)
    [:rejected]
    (let [question-set (first (course/question-sets-for-chapter-quiz course chapter-id))
          fast-route? (if (nil? fast-route?) true fast-route?)]
      [:ok [(events/started course-id chapter-id student-id fast-route?)
            (events/question-assigned course-id chapter-id student-id (:id question-set) (:id (select-random-question question-set previously-seen-questions)))]])))

(defmethod handle-event ::events/Started
  [chapter-quiz {:keys [course-id student-id chapter-id]}]
  (if chapter-quiz
    ;; restarted
    (assoc chapter-quiz
      :number-of-errors 0
      :current-question-set-index -1
      :current-question-set-id nil
      :current-question-id nil
      :fast-route? false
      :running? true)
    (->ChapterQuiz course-id chapter-id student-id nil -1 nil true {} 0)))

(defcommand SubmitAnswer!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  :question-id m/Id
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
    (let [correct-answer-event (events/question-answered-correctly course-id chapter-id student-id question-id inputs)]
      (if (= (inc current-question-set-index)
             (count (course/question-sets-for-chapter-quiz course chapter-id)))

        [:ok [correct-answer-event
              (events/passed course-id chapter-id student-id)]]

        (let [next-question-set (get (course/question-sets-for-chapter-quiz course chapter-id) (inc current-question-set-index))]
          [:ok [correct-answer-event
                (events/question-assigned course-id chapter-id student-id (:id next-question-set) (:id (select-random-question next-question-set previously-seen-questions)))]])))
    [:ok [(events/question-answered-incorrectly course-id chapter-id student-id question-id inputs)]]))

(defmethod handle-event ::events/QuestionAssigned
  [chapter-quiz {:keys [question-set-id question-id]}]
  (-> chapter-quiz
      (update-in [:previously-seen-questions question-set-id] (fnil conj #{}) question-id)
      (assoc :current-question-id question-id)
      (assoc :current-question-set-id question-set-id)
      (update-in [:current-question-set-index] inc)))

(defmethod handle-event ::events/QuestionAnsweredCorrectly
  [chapter-quiz _]
  chapter-quiz)

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
  [{:keys [fast-route? number-of-errors previously-seen-questions current-question-set-index]} {:keys [chapter-id course-id student-id]} course]
  (cond
   (and fast-route?
        (= 2 number-of-errors))
   [:ok [(events/failed course-id chapter-id student-id)
         (events/locked course-id chapter-id student-id)]]

   (= 3 number-of-errors)
   [:ok [(events/failed course-id chapter-id student-id)]]

   (= (inc current-question-set-index)
      (count (course/question-sets-for-chapter-quiz course chapter-id)))
   [:ok [(events/passed course-id chapter-id student-id)]]
   :else (let [next-question-set (get (course/question-sets-for-chapter-quiz course chapter-id) (inc current-question-set-index))]
           [:ok [(events/question-assigned course-id
                                           chapter-id
                                           student-id
                                           (:id next-question-set)
                                           (:id (select-random-question next-question-set
                                                                        previously-seen-questions)))]])))

(defmethod handle-event ::events/Locked
  [chapter-quiz _]
  (assoc chapter-quiz :locked? true))

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

(defmethod aggregate-ids ::section-test/Finished
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-notification ::section-test/Finished
  [chapter-quiz {:keys [section-id course-id chapter-id student-id]} section-test course]
  (let [all-sections (set (map :id (:sections (course/chapter course chapter-id))))]
    [(events/section-finished course-id chapter-id student-id section-id all-sections)]))

(defmethod handle-event ::events/SectionFinished
  [chapter-quiz {:keys [section-id course-id chapter-id student-id all-sections]}]
  (-> chapter-quiz
      (update-in [:finished-sections] (fnil conj #{}) section-id)
      (assoc-in [:all-sections] all-sections)))


(defmethod handle-notification ::events/SectionFinished
  [{:keys [finished-sections all-sections]} {:keys [student-id course-id chapter-id]} _]
  (when (= (set/intersection finished-sections all-sections) all-sections)
    [(events/un-locked course-id chapter-id student-id)]))

(defmethod handle-event ::events/UnLocked
  [chapter-quiz {:keys [course-id chapter-id student-id]}]
  (if chapter-quiz
    ;; restarted
    (assoc chapter-quiz
      :locked? false
      :fast-route? false)
    (->ChapterQuiz course-id chapter-id student-id nil -1 nil false {} 0)))
