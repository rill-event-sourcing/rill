(ns studyflow.school-administration.import-student
  (:require [clojure.java.io :as io]
            [crypto.password.bcrypt :as bcrypt]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id]]
            [studyflow.school-administration.student :as student]
            [studyflow.command-tools :refer [with-claim combine-commands]]
            [rill.message :refer [defcommand]]
            [rill.aggregate :refer [handle-command handle-event update-aggregate]]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defcommand ImportStudent!
  :student-id s/Uuid
  :full-name s/Uuid
  :department-id s/Uuid
  :class-name s/Str
  :email s/Str
  :encrypted-password s/Str)

(defmethod handle-command ::ImportStudent!
  [student {:keys [student-id full-name department-id class-name email encrypted-password]}]
  (combine-commands student
                    (student/create! student-id full-name)
                    (student/change-department! student-id 0 department-id)
                    (student/change-class! student-id 1 class-name)
                    (student/change-credentials! student-id 2 email encrypted-password)))

(defn ok?
  [[status & _]]
  (= :ok status))

(defn import-student
  [event-store department-id student-id {:keys [full-name class-name email password] :as student}]
  {:pre [student-id department-id class-name email password]}
  (if (and email
           (not= "" email))
    (let [encrypted-password (bcrypt/encrypt password)
          result (with-claim event-store
                   (student/claim-email-address! student-id email)
                   (import-student! student-id full-name department-id class-name email encrypted-password)
                   (student/release-email-address! student-id email))]
      (log/info result)
      result)
    [:invalid-email]))

(defn skipped?
  [[status & _]]
  (= :skipped status))


(defn row->student
  [[first-name infix last-name email password class-name]]
  {:full-name (student/full-name first-name infix last-name)
   :email email
   :password password
   :class-name class-name})

(defn import-students
  [event-store department-id students]
  {:pre [department-id students event-store]}
  (let [results (doall (map #(import-student event-store department-id (new-id) %)
                            students))
        total (count students)
        imported (count (filter ok? results))
        errors (filter (complement ok?) results)]
    {:results results
     :total-students total
     :total-imported imported
     :errors errors}))


(defn import-csv-data
  [event-store department-id rows]
  (if-let [data-rows (next rows)]
    (import-students event-store department-id (map row->student data-rows))))

(defn import-tabbed-string
  [event-store department-id tabbed]
  {:pre [event-store department-id tabbed]}
  (import-csv-data event-store department-id (map #(-> %
                                                       string/trim-newline
                                                       (string/split #"\t"))
                                                  (filter (complement string/blank?)
                                                          (string/split tabbed #"\n")))))
