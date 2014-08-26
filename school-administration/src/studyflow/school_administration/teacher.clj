(ns studyflow.school-administration.teacher
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [studyflow.school-administration.teacher.events :as events]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [clojure.set :as set]
            [schema.core :as s]))

(def error-messages {:can-not-be-blank "can't be blank"
                     :already-claimed "already claimed"
                     :ownership-disputed "ownership disputed"})

(defrecord Teacher [id department-id full-name])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; creation

(defcommand Create!
  :teacher-id s/Uuid
  :department-id s/Uuid
  :full-name s/Str)

(defmethod aggregate-ids ::Create!
  [{:keys [department-id]}]
  [department-id])

(defmethod handle-command ::Create!
  [teacher {:keys [teacher-id full-name department-id]} department]
  {:pre [(nil? teacher)]}
  (cond (str/blank? full-name)
        [:rejected {:full-name [(:can-not-be-blank error-messages)]}]
        (not department)
        [:rejected {:department-id [(:can-not-be-blank error-messages)]}]
        :else
        [:ok [(events/created teacher-id department-id full-name)]]))

(defmethod handle-event ::events/Created
  [_ {:keys [teacher-id event full-name department-id]}]
  (->Teacher teacher-id full-name department-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing name

(defcommand ChangeName!
  :teacher-id s/Uuid
  :expected-version s/Int
  :full-name s/Str)

(defmethod handle-command ::ChangeName!
  [teacher {:keys [teacher-id full-name]}]
  {:pre [teacher]}
  (if (str/blank? full-name)
    [:rejected {:full-name [(:can-not-be-blank error-messages)]}]
    [:ok [(events/name-changed teacher-id full-name)]]))

(defmethod handle-event ::events/NameChanged
  [teacher {:keys [full-name]}]
  (assoc teacher :full-name full-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing credentials

(defcommand ChangeCredentials!
  :teacher-id s/Uuid
  :expected-version s/Int
  :email s/Str
  :encrypted-password s/Str)

(defmethod handle-command ::ChangeCredentials!
  [{:keys [has-email-credentials?] :as teacher} {:keys [teacher-id email encrypted-password]}]
  {:pre [teacher]}
  (cond
   (str/blank? email)
   [:rejected {:email [(:can-not-be-blank error-messages)]}]

   encrypted-password
   [:ok [(if has-email-credentials?
           (events/credentials-changed teacher-id {:email email
                                                   :encrypted-password encrypted-password})
           (events/credentials-added teacher-id {:email email
                                                 :encrypted-password encrypted-password}))]]

   :else
   [:ok [(events/email-changed teacher-id email)]]))

(defmethod handle-event ::events/CredentialsChanged
  [teacher {:keys [credentials]}]
  (assoc teacher :credentials credentials))

(defmethod handle-event ::events/CredentialsAdded
  [teacher {:keys [credentials]}]
  (assoc teacher
    :credentials credentials
    :has-email-credentials? true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing email

(defcommand ChangeEmail!
  :teacher-id s/Uuid
  :expected-version s/Int
  :email s/Str)

(defmethod handle-event ::events/EmailChanged
  [teacher {:keys [email]}]
  (assoc-in teacher [:credentials :email] email))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; associate with department

(defcommand ChangeDepartment!
  :teacher-id s/Uuid
  :expected-version s/Int
  :department-id s/Uuid)

(defmethod handle-command ::ChangeDepartment!
  [teacher {:keys [teacher-id department-id]}]
  {:pre [teacher]}
  [:ok [(events/department-changed teacher-id department-id)]])

(defmethod handle-event ::events/DepartmentChanged
  [teacher {:keys [department-id]}]
  (-> teacher
      (assoc :department-id department-id)
      (dissoc :class-names)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; associate with class

(defcommand ChangeClasses!
  :teacher-id s/Uuid
  :expected-version s/Int
  :class-names #{s/Str})

(defmethod handle-command ::ChangeClasses!
  [{original-classes :class-names :keys [department-id] :as teacher} {:keys [teacher-id class-names]}]
  {:pre [teacher]}
  (if department-id
    (let [assigned (map (partial events/class-assigned teacher-id department-id) (set/difference (set class-names) (set original-classes)))
          unassigned (map (partial events/class-unassigned teacher-id department-id) (set/difference (set original-classes)  (set class-names) ))]
      [:ok (concat assigned unassigned)])
    [:rejected {:class-name ["You must set a department before assigning a class"]}]))

(defmethod handle-event ::events/ClassAssigned
  [{:keys [class-names] :as teacher} {:keys [class-name]}]
  (assoc teacher :class-names (conj (set class-names) class-name)))

(defmethod handle-event ::events/ClassUnassigned
  [{:keys [class-names] :as teacher} {:keys [class-name]}]
  (assoc teacher :class-names (dissoc (set class-names) class-name)))


