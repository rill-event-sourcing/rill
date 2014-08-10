(ns studyflow.web.aggregates)

(defn conj-streak [streak question-id result]
  "only update the streak with the first result for each question"
  (if (= (first (peek streak)) question-id)
    (if (= (second (peek streak)) :open)
      (conj (pop streak) [question-id result])
      streak ;; already recorded a streak entry
      )
    (conj streak [question-id result])
    ))

(defn section-test-progress [section-test-agg]
  (cond
   (:finished section-test-agg)
   :finished
   section-test-agg
   :in-progress
   :else ;; nil and false
   :not-started))

(defn finished-last-action [section-test-agg]
  (and (:finished section-test-agg)
       (= (count (:questions section-test-agg))
          (:finished-at section-test-agg))))

(defn handle-event [agg event]
  (condp = (:type event)
    "studyflow.learning.section-test.events/Created"
    (let [aggr-id (:section-id event)]
      {:id aggr-id
       :questions []
       :streak []})
    "studyflow.learning.section-test.events/QuestionAssigned"
    (let [question-id (:question-id event)]
      (-> agg
          (update-in [:questions] conj {:question-id question-id
                                        :question-index (count (:questions agg))})
          (update-in [:streak]
                     conj [question-id :open])))
    "studyflow.learning.section-test.events/AnswerRevealed"
    (let [question-id (:question-id event)
          answer (:answer event)]
      (-> agg
          (update-in [:streak]
                     conj-streak question-id :revealed)
          (update-in [:questions]
                     (fn [qs]
                       (vec (for [q qs]
                              (if (= question-id (:question-id q))
                                (assoc q
                                  :worked-out-answer answer)
                                q)))))))
    "studyflow.learning.section-test.events/QuestionAnsweredCorrectly"
    (let [question-id (:question-id event)
          inputs (:inputs event)]
      (-> agg
          (update-in [:streak]
                     conj-streak question-id :correct)
          (update-in [:questions]
                     (fn [qs]
                       (vec (for [q qs]
                              (if (= question-id (:question-id q))
                                (assoc q
                                  :correct true
                                  :inputs inputs)
                                q)))))))
    "studyflow.learning.section-test.events/QuestionAnsweredIncorrectly"
    (let [question-id (:question-id event)
          inputs (:inputs event)]
      (-> agg
          (update-in [:streak]
                     conj-streak question-id :incorrect)
          (update-in [:questions]
                     (fn [qs]
                       (vec (for [q qs]
                              (if (= question-id (:question-id q))
                                (assoc q
                                  :correct false
                                  :inputs inputs)
                                q)))))))
    "studyflow.learning.section-test.events/Finished"
    (assoc agg
      :finished true
      :finished-at (count (:questions agg)))

    "studyflow.learning.student-entry-quiz.events/Created"
    (let [aggr-id (:entry-quiz-id event)]
      {:id aggr-id
       :questions []
       :status :created})
    (do
      (prn "Aggregate can't handle event: " event)
      agg)))

(defn apply-events [agg aggregate-version events]
  (-> (reduce
       handle-event
       agg
       events)
      (assoc :aggregate-version aggregate-version)))

