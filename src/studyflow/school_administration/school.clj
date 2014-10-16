(ns studyflow.school-administration.school
  (:require [clojure.string :as str]
            [rill.aggregate :refer [handle-command handle-event]]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [schema.core :as s]
            [studyflow.school-administration.school.events :as events]))

(defrecord School [id name brin])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; creation

(defcommand Create!
  :school-id s/Uuid
  :name s/Str
  :brin s/Str)

(defmethod handle-command ::Create!
  [school {:keys [school-id name brin]}]
  {:pre [(nil? school)]}
  (let [errors (reduce merge
                       (map (fn [[k v]] (if (= "" (str/trim v)) {k ["can't be blank"]}))
                            {:name name, :brin brin}))]
    (if (seq errors)
      [:rejected errors]
      [:ok [(events/created school-id name brin)]])))

(defmethod handle-event ::events/Created
  [_ event]
  (->School (:school-id event) (:name event) (:brin name)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing name

(defcommand ChangeName!
  :school-id s/Uuid
  :expected-version s/Int
  :name s/Str)

(defmethod handle-command ::ChangeName!
  [school {:keys [school-id name]}]
  {:pre [school]}
  (if (= "" (str/trim name))
    [:rejected {:name ["can't be blank"]}]
    [:ok [(events/name-changed school-id name)]]))

(defmethod handle-event ::events/NameChanged
  [school event]
  (assoc school :name (:name event)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; claimig a BRIN

(defcommand ClaimBrin!
  :owner-id s/Uuid
  :brin s/Str)

(defmethod primary-aggregate-id ::ClaimBrin!
  [command]
  (str "brin-ownership-" (:brin command)))

(defmethod primary-aggregate-id ::events/BrinClaimed
  [event]
  (str "brin-ownership-" (:brin event)))

(defmethod handle-command ::ClaimBrin!
  [{:keys [current-owner-id]} {:keys [owner-id brin]}]
  (if (or (nil? current-owner-id)
          (= owner-id current-owner-id))
    [:ok [(events/brin-claimed owner-id brin)]]
    [:rejected {:brin ["already claimed"]}]))

(defmethod handle-event ::events/BrinClaimed
  [claim event]
  (assoc claim :current-owner-id (:owner-id event)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; releasing a BRIN

(defcommand ReleaseBrin!
  :owner-id s/Uuid
  :brin s/Str)

(defmethod primary-aggregate-id ::ReleaseBrin!
  [command]
  (str "brin-ownership-" (:brin command)))

(defmethod primary-aggregate-id ::events/BrinReleased
  [event]
  (str "brin-ownership-" (:brin event)))

(defmethod handle-command ::ReleaseBrin!
  [{:keys [current-owner-id]} {:keys [owner-id brin]}]
  (if (= current-owner-id owner-id)
    [:ok [(events/brin-released owner-id brin)]]
    [:rejected {:brin ["ownership disputed"]}]))

(defmethod handle-event ::events/BrinReleased
  [claim event]
  (dissoc claim :current-owner-id))
