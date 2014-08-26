(ns studyflow.school-administration.teacher
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [studyflow.school-administration.teacher.events :as events]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [schema.core :as s]))

(def error-messages {:can-not-be-blank "can't be blank"
                     :already-claimed "already claimed"
                     :ownership-disputed "ownership disputed"})

(defrecord Teacher [id full-name])

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
        [:ok [(events/created teacher-id full-name)]]))

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
;; claiming an email adress

(defcommand ClaimEmailAddress!
  :owner-id s/Uuid
  :email s/Str)

(defmethod primary-aggregate-id ::ClaimEmailAddress!
  [{:keys [email]}]
  (str "email-ownership-" email))

(defmethod primary-aggregate-id ::events/EmailAddressClaimed
  [{:keys [email]}]
  (str "email-ownership-" email))

(defmethod handle-command ::ClaimEmailAddress!
  [{:keys [current-owner-id]} {:keys [owner-id email]}]
  (if (or (nil? current-owner-id)
          (= owner-id current-owner-id))
    [:ok [(events/email-address-claimed owner-id email)]]
    [:rejected {:email [(:already-claimed error-messages)]}]))

(defmethod handle-event ::events/EmailAddressClaimed
  [claim {:keys [owner-id]}]
  (assoc claim :current-owner-id owner-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; releasing an email adress

(defcommand ReleaseEmailAddress!
  :owner-id s/Uuid
  :email s/Str)

(defmethod primary-aggregate-id ::ReleaseEmailAddress!
  [command]
  (str "email-ownership-" (:email command)))

(defmethod primary-aggregate-id ::events/EmailAddressReleased
  [event]
  (str "email-ownership-" (:email event)))

(defmethod handle-command ::ReleaseEmailAddress!
  [{:keys [current-owner-id]} {:keys [owner-id email]}]
  (if (= current-owner-id owner-id)
    [:ok [(events/email-address-released owner-id email)]]
    [:rejected {:email [(:ownership-disputed error-messages)]}]))

(defmethod handle-event ::events/EmailAddressReleased
  [claim event]
  (dissoc claim :current-owner-id))


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
      (dissoc :class-name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; associate with class

(defcommand ChangeClass!
  :teacher-id s/Uuid
  :expected-version s/Int
  :class-name s/Str)

(defmethod handle-command ::ChangeClass!
  [{original-class-name :class-name :keys [department-id] :as teacher} {:keys [teacher-id class-name]}]
  {:pre [teacher]}
  (if department-id
    [:ok [(events/class-assigned teacher-id department-id class-name)]]
    [:rejected {:class-name ["You must set a department before assigning a class"]}]))

(defmethod handle-event ::events/ClassAssigned
  [teacher {:keys [class-name]}]
  (assoc teacher :class-name class-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Importing teachers

(defcommand ImportTeacher!
  :teacher-id s/Uuid
  :full-name s/Uuid
  :department-id s/Uuid
  :class-name s/Str
  :email s/Str
  :encrypted-password s/Str)

(defn validate-with [pred msg rec ks & [errors]]
  (reduce (fn [m k] (update-in m [k] conj msg))
          (or errors {})
          (filter (complement #(pred (get rec %))) ks)))

(defmethod handle-command ::ImportTeacher!
  [teacher {:keys [teacher-id full-name department-id class-name email encrypted-password] :as import}]
  {:pre [(nil? teacher)]}
  (let [errors (validate-with (complement #(str/blank? (str %)))
                              (:can-not-be-blank error-messages)
                              import
                              [:full-name :department-id :class-name :email :encrypted-password])]
    (if (seq errors)
      [:rejected errors]
      [:ok [(events/imported teacher-id
                             full-name
                             department-id
                             class-name
                             {:email email
                              :encrypted-password encrypted-password})]])))
