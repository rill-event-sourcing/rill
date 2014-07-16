(ns studyflow.school-administration.student
  (:require [studyflow.school-administration.student.events :as events]
            [rill.aggregate :refer [defaggregate handle-event handle-command aggregate-ids]]
            [rill.message :refer [defcommand]]
            [schema.core :as s]))

(defaggregate Student [full-name])

(defcommand Create!
  :student-id s/Uuid
  :full-name s/Str)

(defmethod handle-command ::Create!
  [student {:keys [student-id full-name]}]
  {:pre [(nil? student)]}
  [(events/created student-id full-name)])

(defmethod handle-event ::events/Created
  [_ event]
  (->Student (:student-id event) (:full-name event)))

(defcommand ChangeName!
  :student-id s/Uuid
  :expected-version s/Int
  :full-name s/Str)

(defmethod handle-command ::ChangeName!
  [student {:keys [student-id full-name]}]
  {:pre [student]}
  [(events/name-changed student-id full-name)])

(defmethod handle-event ::events/NameChanged
  [student event]
  (assoc student :full-name (:full-name event)))

(defcommand CreateFromEduRouteCredentials!
  :student-id s/Uuid
  :edu-route-id s/Str
  :full-name s/Str)

(defmethod handle-command ::CreateFromEduRouteCredentials!
  [student {:keys [student-id full-name edu-route-id]}]
  {:pre [(nil? student)]}
  [(events/created student-id full-name)
   (events/edu-route-credentials-added student-id edu-route-id)])

