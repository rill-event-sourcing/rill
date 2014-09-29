(ns studyflow.learning.tracking
  (:require [clojure.tools.logging :as log]
            [studyflow.learning.tracking.events :as events]
            [studyflow.learning.tracking.commands :as commands]
            [rill.aggregate :refer [handle-event handle-command]]
            [rill.uuid :refer [uuid]]))

(defmethod handle-event ::events/DashboardNavigated
  [tracking event]
  tracking)

(defmethod handle-event ::events/EntryQuizNavigated
  [tracking event]
  tracking)

(defmethod handle-event ::events/SectionExplanationNavigated
  [tracking event]
  tracking)

(defmethod handle-event ::events/SectionTestNavigated
  [tracking event]
  tracking)


(defmethod handle-command ::commands/Navigate!
  [agg {:keys [student-id data]}]
  {:pre [student-id]}
  (if-let [event (condp = (keyword (:main data))
                   :dashboard (events/dashboard-navigated student-id)
                   :entry-quiz (events/entry-quiz-navigated student-id)
                   :learning
                   (if (= (keyword (:section-tab data)) :explanation)
                     (events/section-explanation-navigated student-id (uuid (:section-id data)))
                     (events/section-test-navigated student-id (uuid (:section-id data))))
                   nil)]
    [:ok [event]]
    [:rejected {:message "unknown navigation type"}]))
