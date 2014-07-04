(ns studyflow.web.aggregates)

(defn handle-event [agg event]
  (condp = (:type event)
    "studyflow.learning.section-test.events/Created"
    (let [aggr-id (:section-test-id event)]
      {:id aggr-id
       :questions []
       :streak []})
    "studyflow.learning.section-test.events/QuestionAssigned"
    (let [question-id (:question-id event)]
      (update-in agg [:questions] conj {:question-id question-id}))
    "studyflow.learning.section-test.events/QuestionAnsweredCorrectly"
    (let [question-id (:question-id event)
          inputs (:inputs event)]
      (-> agg
          (update-in [:streak] conj :correct)
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
          (update-in [:streak] conj :incorrect)
          (update-in [:questions]
                     (fn [qs]
                       (vec (for [q qs]
                              (if (= question-id (:question-id q))
                                (assoc q
                                  :correct false
                                  :inputs inputs)
                                q)))))))))

(defn apply-events [agg events]
  (reduce
   handle-event
   agg
   events))

