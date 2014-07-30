(ns studyflow.school-administration.department
  (:require [clojure.string :as str]
            [rill.aggregate :refer [handle-command handle-event]]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [schema.core :as s]
            [studyflow.school-administration.department.events :as events]))

(defrecord Department [id school-id name])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; creation

(defcommand Create!
  :department-id s/Uuid
  :school-id s/Uuid
  :name s/Str)

(defmethod handle-command ::Create!
  [department {:keys [department-id school-id name]}]
  {:pre [(nil? department)]}
  (if (= "" (str/trim name))
    [:rejected {:name ["can't be blank"]}]
    [:ok [(events/created department-id school-id name)]]))

(defmethod handle-event ::events/Created
  [_ event]
  (->Department (:department-id event) (:school-id event) (:name event)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing name

(defcommand ChangeName!
  :department-id s/Uuid
  :expected-version s/Int
  :name s/Str)

(defmethod handle-command ::ChangeName!
  [department {:keys [department-id name]}]
  {:pre [department]}
  (if (= "" (str/trim name))
    [:rejected {:name ["can't be blank"]}]
    [:ok [(events/name-changed department-id name)]]))

(defmethod handle-event ::events/NameChanged
  [department event]
  (assoc department :name (:name event)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing sales data

(defcommand ChangeSalesData!
  :department-id s/Uuid
  :expected-version s/Int
  :licenses-sold s/Int
  :status s/Str)

(defmethod handle-command ::ChangeSalesData!
  [department {:keys [department-id licenses-sold status]}]
  {:pre [department]}
  [:ok [(events/sales-data-changed department-id licenses-sold status)]])

(defmethod handle-event ::events/SalesDataChanged
  [department event]
  (assoc department :sales-data (select-keys event [:licenses-sold :status])))
