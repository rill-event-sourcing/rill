(ns studyflow.learning.chapter-quiz
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [studyflow.learning.chapter-quiz.events :as events :refer [chapter-quiz-id]]
            [studyflow.learning.chapter-quiz.notifications :refer [notify]]
            [studyflow.learning.section-test.events :as section-test]
            [rill.message :refer [defcommand]]
            [studyflow.learning.course-material :as m]
            [studyflow.learning.course :as course]
            [studyflow.rand :refer [*rand-nth*]]
            [schema.core :as s]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defrecord ChapterQuiz
    [course-id
     chapter-id
     student-id
     current-question-id
     current-question-set-index
     current-question-set-id
     state
     previously-seen-questions ;; {question-set-id => [ first-seen-question-id second-seen-question-id ... ]}
     number-of-errors])

(def transitions
  "current-state -> event -> new-state || nil

  use user/view-transitions to generate diagram"
  {nil                 {:started   :running-fast-track
                        :un-locked :un-locked}
   :running-fast-track {:passed    :passed
                        :stopped   :locked
                        :failed    :locked
                        ;; This should never happen
                        ;; but if it does, better to break off the running test
                        ;; otherwise we'll never unlock the chapter
                        :un-locked :un-locked}
   :locked             {:un-locked :un-locked}
   :un-locked          {:started   :running}
   :running            {:passed    :passed
                        :stopped   :un-locked
                        :failed    :un-locked}})

(defn transition
  [chapter-quiz event]
  (update-in chapter-quiz [:state]
             (fn [old-state]
               (get-in transitions [old-state event] old-state))))

(defcommand Start!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::Start!
  [{:keys [course-id]}]
  [course-id])


(defn select-random-question
  [{:keys [questions] :as question-set} previously-seen-questions]
  (let [dont-pick-these (if (< (count questions) (count previously-seen-questions))
                          (set previously-seen-questions)
                          (if (or (<= 4 (count questions))
                                  (<= 2 (count previously-seen-questions)))
                            #{}
                            (set (take-last (- (count questions) 2) previously-seen-questions))))
        available-questions (filter (fn [question] (not (dont-pick-these (:id question))))
                                    questions)]
    (assert (seq available-questions))
    (*rand-nth* (vec available-questions))))

(defmethod handle-command ::Start!
  [{:keys [previously-seen-questions state] :as chapter-quiz} {:keys [student-id course-id chapter-id]} course]
  (if (or (nil? state)
          (= :un-locked state))
    (let [question-set (first (course/question-sets-for-chapter-quiz course chapter-id))]
      [:ok [(events/started course-id chapter-id student-id (nil? state))
            (events/question-assigned course-id chapter-id student-id (:id question-set) (:id (select-random-question question-set previously-seen-questions)))]])
    [:rejected]))

(defmethod handle-event ::events/Started
  [{:keys [state] :as chapter-quiz} {:keys [course-id student-id chapter-id]}]
  (if chapter-quiz
    ;; restarted
    (-> chapter-quiz
        (assoc :number-of-errors 0
               :current-question-set-index -1
               :current-question-set-id nil
               :current-question-id nil)
        (transition :started))
    (->ChapterQuiz course-id chapter-id student-id nil -1 nil :running-fast-track {} 0)))

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
  [{:keys [current-question-id current-question-set-index course-id chapter-id number-of-errors student-id previously-seen-questions] :as chapter-quiz} {:keys [inputs question-id] :as command} course]
  {:pre [(= current-question-id question-id)]}
  (if (course/answer-correct? (course/question-for-chapter-quiz course chapter-id current-question-id) inputs)
    (let [correct-answer-event (events/question-answered-correctly course-id chapter-id student-id question-id inputs)]
      (if (= (inc current-question-set-index)
             (count (course/question-sets-for-chapter-quiz course chapter-id)))

        [:ok [correct-answer-event
              (events/passed course-id chapter-id student-id)]]

        (let [next-question-set (get (course/question-sets-for-chapter-quiz course chapter-id) (inc current-question-set-index))]
          [:ok [correct-answer-event
                (events/question-assigned course-id chapter-id student-id (:id next-question-set)
                                          (:id (select-random-question next-question-set previously-seen-questions)))]])))
    [:ok [(events/question-answered-incorrectly course-id chapter-id student-id question-id inputs)]]))

(defmethod handle-event ::events/QuestionAssigned
  [chapter-quiz {:keys [question-set-id question-id]}]
  (-> chapter-quiz
      (update-in [:previously-seen-questions question-set-id] (fnil conj []) question-id)
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
  (transition chapter-quiz :passed))

(defmethod handle-event ::events/Failed
  [chapter-quiz _]
  (transition chapter-quiz :failed))

(defcommand DismissErrorScreen!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::DismissErrorScreen!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::DismissErrorScreen!
  [{:keys [state number-of-errors previously-seen-questions current-question-set-index]} {:keys [chapter-id course-id student-id]} course]
  (cond
   (and (= :running-fast-track state)
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
  (transition chapter-quiz :locked))

(defcommand Stop!
  :course-id m/Id
  :chapter-id m/Id
  :student-id m/Id
  chapter-quiz-id)

(defmethod aggregate-ids ::Stop!
  [{:keys [course-id]}]
  [course-id])

(defmethod handle-command ::Stop!
  [{:keys [state]} {:keys [student-id course-id chapter-id]} course]
  (if (= :running-fast-route state)
    [:ok [(events/stopped course-id chapter-id student-id)
          (events/locked course-id chapter-id student-id)]]
    [:ok [(events/stopped course-id chapter-id student-id)]]))

(defmethod handle-event ::events/Stopped
  [chapter-quiz _]
  (transition chapter-quiz :stopped))

(defmethod aggregate-ids ::section-test/Finished
  [{:keys [course-id]}]
  [course-id])

(defmethod notify ::section-test/Finished
  [chapter-quiz {:keys [section-id course-id chapter-id student-id]} section-test course]
  (let [all-sections (set (map :id (:sections (course/chapter course chapter-id))))]
    [(events/section-finished course-id chapter-id student-id section-id all-sections)]))

(defmethod handle-event ::events/SectionFinished
  [chapter-quiz {:keys [section-id course-id chapter-id student-id all-sections]}]
  (-> (or chapter-quiz
          (->ChapterQuiz course-id chapter-id student-id nil -1 nil nil {} 0))
      (update-in [:finished-sections] (fnil conj #{}) section-id)
      (assoc-in [:all-sections] all-sections)))

(defmethod notify ::events/SectionFinished
  [{:keys [finished-sections all-sections]} {:keys [student-id course-id chapter-id]} _]
  (when (= (set/intersection finished-sections all-sections) all-sections)
    [(events/un-locked course-id chapter-id student-id)]))

(defmethod handle-event ::events/UnLocked
  [chapter-quiz {:keys [course-id chapter-id student-id]}]
  (if chapter-quiz
    ;; restarted
    (transition chapter-quiz :un-locked)
    (->ChapterQuiz course-id chapter-id student-id nil -1 nil :un-locked {} 0)))
