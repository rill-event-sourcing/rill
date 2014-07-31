(ns studyflow.school-administration.import-student
  (:require [clojure.java.io :as io]
            [crypto.password.bcrypt :as bcrypt]
            [rill.handler :refer [try-command]]
            [rill.uuid :refer [new-id]]
            [studyflow.school-administration.student :as student]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn import-student-commands
  [department-id student-id {:keys [full-name email password class-name]}]
  {:pre [full-name email password class-name]}
  [(student/create! student-id full-name)
   (student/change-department! student-id 0 department-id)
   (student/change-class! student-id 1 class-name)
   (student/change-credentials! student-id 2 email (bcrypt/encrypt password))])

(defn ok?
  [[status & _]]
  (= :ok status))

(defn try-all
  "try all commands in order until done or one of them is not ok

  Returns the result of the last executed command"
  [event-store [command & commands]]
  (let [result (try-command event-store command)]
    (if (ok? result)
      (if (seq? commands)
        (recur event-store commands)
        result)
      result)))

(defn import-student
  [event-store department-id student-id {:keys [email] :as student}]
  (if (and email
           (not= "" email))
    (if (ok? (try-command event-store (student/claim-email-address! student-id email)))
      (let [result (try-all event-store (import-student-commands department-id student-id student))]
        (if (ok? result)
          result
          (do (try-command event-store (student/release-email-address! student-id email))
              result)))
      [:skipped])
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
  (let [results (doall (map #(import-student event-store department-id (new-id) %)
                            students))
        total (count students)
        imported (count (filter ok? results))
        skipped  (count (filter skipped? results))
        errors (- total imported skipped)]
    {:results results
     :total-students total
     :total-imported imported
     :total-skipped skipped
     :total-errors errors}))


(defn import-csv-data
  [event-store department-id rows]
  (if-let [data-rows (next rows)]
    (import-students event-store department-id (map row->student data-rows))))

(defn import-tabbed-string
  [event-store department-id tabbed]
  (import-csv-data event-store department-id (map #(-> %
                                                       string/trim-newline
                                                       (string/split #"\t"))
                                                  (filter (complement string/blank?)
                                                          (string/split tabbed #"\n")))))
