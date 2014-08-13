(ns studyflow.learning.student-entry-quiz
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.course :as course]
            [studyflow.learning.student-entry-quiz.events :as events]
            [studyflow.learning.student-entry-quiz.commands :as commands]
            [studyflow.learning.entry-quiz-material :as entry-quiz-material]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.uuid :refer [new-id]]))

(defn passed? [student-entry-quiz entry-quiz-material]
  (->> (map
        (fn [question]
          (let [input-values (get-in student-entry-quiz [:answers (:id question)])]
            (if (course/answer-correct? question input-values)
              :correct
              :incorrect)))
        (:questions entry-quiz-material))
       (filter #{:correct})
       count
       (<= 2))) ;; TODO threshold from entry-quiz-material

(defn next-question
  ([entry-quiz-material]
     (let [questions (:questions entry-quiz-material)]
       (first questions)))
  ([entry-quiz-material current-question-id]
     (let [questions (:questions entry-quiz-material)]
       (->> (:questions entry-quiz-material)
            (partition 2 1)
            (some (fn [[this next]]
                    (when (= (:id this) current-question-id)
                      next)))))))

(defn remove-answers [question]
  (-> question
      (update-in [:line-input-fields]
                 (fn [lis]
                   (mapv #(dissoc % :correct-answers) lis)))
      (update-in [:multiple-choice-input-fields]
                 (fn [mcis]
                   (mapv (fn [mc]
                           (update-in mc [:choices]
                                      (partial mapv #(dissoc % :correct))))
                         mcis)))))

(defn append-question-data [event question]
  (let [save-question (remove-answers question)]
    (assoc event :question-data save-question)))

(defmethod aggregate-ids ::commands/Init!
  [{:keys [entry-quiz-id]}]
  [entry-quiz-id])

(defmethod handle-command ::commands/Init!
  [entry-quiz {:keys [entry-quiz-id student-id] :as command} entry-quiz-material]
  {:pre [(nil? entry-quiz)
         entry-quiz-id
         student-id
         entry-quiz-material]}
  (let [question (next-question entry-quiz-material)]
    [:ok [(events/created entry-quiz-id student-id)
          (-> (events/question-assigned entry-quiz-id student-id (:id question))
              (append-question-data question))]]))

(defmethod handle-event ::events/Created
  [_ {:keys [entry-quiz-id student-id] :as event}]
  {:entry-quiz-id entry-quiz-id
   :student-id student-id
   :status :started
   :current-question-index nil
   :answers {} ;; question-id -> inputs
   })

(defmethod handle-event ::events/QuestionAssigned
  [this {:keys [question-id] :as event}]
  (-> this
      (assoc :current-question-id question-id)))

(defmethod aggregate-ids ::commands/SubmitAnswer!
  [{:keys [entry-quiz-id]}]
  [entry-quiz-id])

(defmethod handle-command ::commands/SubmitAnswer!
  [{:keys [current-question-id entry-quiz-id student-id] :as this}
   {:keys [inputs question-id] :as command} entry-quiz-material]
  {:pre [(= current-question-id question-id)]}
  [:ok [(events/question-answered entry-quiz-id student-id question-id inputs)
        (if-let [question (next-question entry-quiz-material current-question-id)]
          (-> (events/question-assigned entry-quiz-id student-id (:id question))
              (append-question-data question))
          (let [this-with-current-answer
                (assoc-in this [:answers question-id] inputs)]
            (if (passed? this-with-current-answer entry-quiz-material)
              (events/quiz-passed entry-quiz-id student-id)
              (events/quiz-failed entry-quiz-id student-id))))]])

(defmethod handle-event ::events/QuestionAnswered
  [this {:keys [question-id inputs] :as event}]
  (-> this
      (assoc-in [:answers question-id] inputs)))
