(ns studyflow.school-administration.student
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [studyflow.school-administration.student.events :as events :refer [edu-route-claim-id]]
            [rill.aggregate :refer [handle-event handle-command aggregate-ids]]
            [rill.message :refer [defcommand primary-aggregate-id]]
            [schema.core :as s]))

(def error-messages {:can-not-be-blank "can't be blank"
                     :already-claimed "already claimed"
                     :ownership-disputed "ownership disputed"})

(defrecord Student [id full-name])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; creation

(defcommand Create!
  :student-id s/Uuid
  :full-name s/Str)

(defmethod handle-command ::Create!
  [student {:keys [student-id full-name]}]
  {:pre [(nil? student)]}
  (if (str/blank? full-name)
    [:rejected {:full-name [(:can-not-be-blank error-messages)]}]
    [:ok [(events/created student-id full-name)]]))

(defmethod handle-event ::events/Created
  [_ {:keys [student-id event full-name]}]
  (->Student student-id full-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing name

(defcommand ChangeName!
  :student-id s/Uuid
  :expected-version s/Int
  :full-name s/Str)

(defmethod handle-command ::ChangeName!
  [student {:keys [student-id full-name]}]
  {:pre [student]}
  (if (str/blank? full-name)
    [:rejected {:full-name [(:can-not-be-blank error-messages)]}]
    [:ok [(events/name-changed student-id full-name)]]))

(defmethod handle-event ::events/NameChanged
  [student {:keys [full-name]}]
  (assoc student :full-name full-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing credentials

(defcommand ChangeCredentials!
  :student-id s/Uuid
  :expected-version s/Int
  :email s/Str
  :encrypted-password s/Str)

(defmethod handle-command ::ChangeCredentials!
  [{:keys [has-email-credentials?] :as student} {:keys [student-id email encrypted-password]}]
  {:pre [student]}
  (cond
   (str/blank? email)
   [:rejected {:email [(:can-not-be-blank error-messages)]}]

   encrypted-password
   [:ok [(if has-email-credentials?
           (events/credentials-changed student-id {:email email
                                                   :encrypted-password encrypted-password})
           (events/credentials-added student-id {:email email
                                                 :encrypted-password encrypted-password}))]]

   :else
   [:ok [(events/email-changed student-id email)]]))

(defmethod handle-event ::events/CredentialsChanged
  [student {:keys [credentials]}]
  (assoc student :credentials credentials))

(defmethod handle-event ::events/CredentialsAdded
  [student {:keys [credentials]}]
  (assoc student
    :credentials credentials
    :has-email-credentials? true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create via edu route

(defcommand ClaimEduRouteId!
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defmethod handle-command ::ClaimEduRouteId!
  [{:keys [current-owner-id]} {:keys [owner-id edu-route-id]}]
  (if (or (nil? current-owner-id)
          (= owner-id current-owner-id))
    [:ok [(events/edu-route-id-claimed edu-route-id owner-id)]]
    [:rejected {:edu-route-id [(:already-claimed error-messages)]}]))

(defcommand ReleaseEduRouteId!
  :edu-route-id s/Str
  :owner-id s/Uuid
  edu-route-claim-id)

(defmethod handle-command ::ReleaseEduRouteId!
  [{:keys [current-owner-id]} {:keys [owner-id edu-route-id]}]
  (if (= current-owner-id owner-id)
    [:ok [(events/edu-route-id-released owner-id edu-route-id)]]
    [:rejected {:edu-route-id [(:ownership-disputed error-messages)]}]))

(defmethod handle-event ::events/EduRouteIdClaimed
  [_ {:keys [owner-id edu-route-id]}]
  {:current-owner-id owner-id})

(defmethod handle-event ::events/EduRouteIdReleased
  [claim {:keys [owner-id edu-route-id]}]
  (dissoc claim :current-owner-id))

(defcommand CreateFromEduRouteCredentials!
  :student-id s/Uuid
  :edu-route-id s/Str
  :full-name s/Str)

(defmethod handle-command ::CreateFromEduRouteCredentials!
  [student {:keys [student-id full-name edu-route-id]}]
  {:pre [(nil? student)]}
  [:ok [(events/created student-id full-name)
        (events/edu-route-credentials-added student-id edu-route-id)]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; changing email

(defcommand ChangeEmail!
  :student-id s/Uuid
  :expected-version s/Int
  :email s/Str)

(defmethod handle-event ::events/EmailChanged
  [student {:keys [email]}]
  (assoc-in student [:credentials :email] email))


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
  :student-id s/Uuid
  :expected-version s/Int
  :department-id s/Uuid)

(defmethod handle-command ::ChangeDepartment!
  [student {:keys [student-id department-id]}]
  {:pre [student]}
  [:ok [(events/department-changed student-id department-id)]])

(defmethod handle-event ::events/DepartmentChanged
  [student {:keys [department-id]}]
  (-> student
      (assoc :department-id department-id)
      (dissoc :class-name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; associate with class

(defcommand ChangeClass!
  :student-id s/Uuid
  :expected-version s/Int
  :class-name s/Str)

(defmethod handle-command ::ChangeClass!
  [{original-class-name :class-name :keys [department-id] :as student} {:keys [student-id class-name]}]
  {:pre [student]}
  (if department-id
    [:ok [(events/class-assigned student-id department-id class-name)]]
    [:rejected {:class-name ["You must set a department before assigning a class"]}]))

(defmethod handle-event ::events/ClassAssigned
  [student {:keys [class-name]}]
  (assoc student :class-name class-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Importing students

(defcommand ImportStudent!
  :student-id s/Uuid
  :full-name s/Uuid
  :department-id s/Uuid
  :class-name s/Str
  :email s/Str
  :encrypted-password s/Str)

(defmethod handle-command ::ImportStudent!
  [student {:keys [student-id full-name department-id class-name email encrypted-password] :as attrs}]
  {:pre [(nil? student)]}
  (let [errors (reduce (fn [m k] (assoc m k [(:can-not-be-blank error-messages)]))
                       {}
                       (filter #(str/blank? (str (get attrs %)))
                               [:full-name :department-id :class-name :email :encrypted-password]))]
    (if (seq errors)
      [:rejected errors]
      [:ok [(events/imported student-id
                             full-name
                             department-id
                             class-name
                             {:email email
                              :encrypted-password encrypted-password})]])))
