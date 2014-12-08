(ns studyflow.learning.section-bank
  (:require [rill.aggregate :refer [handle-notification handle-event]]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.section-bank.events :as events]
            [studyflow.learning.section-bank.notifications :refer [notify]]))

(def max-coins-per-section 60)

(defmethod notify ::section-test/QuestionAnsweredCorrectly
  [{:keys [total-coins-earned]} {:keys [section-id student-id]} {:keys [question-state test-finished? course-id]}]
  (let [total-coins-earned (or total-coins-earned 0)]
    (if (and (= question-state :correct)
             (not test-finished?)
             (< total-coins-earned max-coins-per-section))
      [(events/coins-earned section-id student-id course-id (min 3 (- max-coins-per-section total-coins-earned)))])))

(defmethod handle-event ::events/CoinsEarned
  [section-bank {:keys [amount]}]
  (update-in section-bank [:total-coins-earned] (fnil + 0) amount))

