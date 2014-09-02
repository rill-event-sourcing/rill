(ns studyflow.school-administration.import-student
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [crypto.password.bcrypt :as bcrypt]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id]]
            [studyflow.school-administration.student :as student]
            [studyflow.credentials.email-ownership :as email-ownership]
            [studyflow.command-tools :refer [with-claim]]
            [rill.message :refer [defcommand]]
            [rill.aggregate :refer [handle-command handle-event update-aggregate]]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn ok?
  [[status & _]]
  (= :ok status))

(defn import-student
  [event-store department-id student-id {:keys [full-name class-name email password] :as student}]
  (if (not (str/blank? email))
    (let [encrypted-password (when-not (str/blank? password) (bcrypt/encrypt password))
          result (with-claim event-store
                   (email-ownership/claim! student-id email)
                   (student/import-student! student-id full-name department-id class-name email encrypted-password)
                   (email-ownership/release! student-id email))]
      (log/debug result)
      result)
    [:rejected {:email :invalid}]))

(defn skipped?
  [[status & _]]
  (= :skipped status))

(defn full-name
  [first-name infix last-name]
  (if-not (str/blank? (str infix))
    (str first-name " " infix " " last-name)
    (str first-name " " last-name)))

(defn row->student
  [[first-name infix last-name email password class-name]]
  {:full-name (full-name first-name infix last-name)
   :email email
   :password password
   :class-name class-name})

(defn import-students
  [event-store department-id students]
  (let [results (->> students
                     (map #(import-student event-store department-id (new-id) %))
                     doall)
        total (count students)
        imported (count (filter ok? results))
        errors (filter identity
                       (map (fn [result student]
                              (when-not (ok? result)
                                (concat [(:email student)] (next result))))
                            results
                            students))]
    {:results results
     :total-students total
     :total-imported imported
     :errors errors}))

(defn import-rows
  [event-store department-id rows]
  (if-let [data-rows (next rows)] ; skip the header
    (import-students event-store department-id (map row->student data-rows))))

(defn import-tabbed-string
  [event-store department-id tabbed]
  {:pre [event-store department-id tabbed]}
  (let [lines (filter (complement string/blank?) (string/split tabbed #"\n"))
        rows (map #(-> % string/trim-newline (string/split #"\t")) lines)]
    (import-rows event-store department-id rows)))
