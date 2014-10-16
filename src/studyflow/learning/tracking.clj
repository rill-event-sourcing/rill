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
  [agg {:keys [student-id tracking-location]}]
  {:pre [student-id]}
  (if-let [event (condp = (keyword (:main tracking-location))
                   :dashboard (events/dashboard-navigated student-id)
                   :entry-quiz (events/entry-quiz-navigated student-id)
                   :learning
                   (if (= (keyword (:section-tab tracking-location)) :explanation)
                     (events/section-explanation-navigated student-id (uuid (:section-id tracking-location)))
                     (events/section-test-navigated student-id (uuid (:section-id tracking-location))))
                   nil)]
    [:ok [event]]
    [:rejected {:message "unknown navigation type"}]))
