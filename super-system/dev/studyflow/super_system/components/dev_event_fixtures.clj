(ns studyflow.super-system.components.dev-event-fixtures
  (:require [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :refer [Lifecycle]]
            [clojure.string :as string]
            [clojure.tools.logging :refer [info debug spy]]
            [rill.event-store :as event-store]
            [rill.message :as message]
            [rill.uuid :as uuid]
            [studyflow.school-administration.student.events :as student]
            [studyflow.school-administration.teacher.events :as teacher]
            [studyflow.school-administration.school.events :as school]
            [studyflow.school-administration.department.events :as department]))

(defrecord DevEventFixturesComponent [event-store]
  Lifecycle
  (start [component]
    (info "Loading event fixtures")
    (let [student-id (uuid/new-id)
          school-id (uuid/new-id)
          department-id (uuid/new-id)
          teacher-id (uuid/new-id)
          events [(student/created student-id "Dev Student One")
                  (student/credentials-added student-id
                                             {:email "dev-student-one@studyflow.nl"
                                              :encrypted-password (bcrypt/encrypt "student")})
                  (school/created school-id "Programmer School 1" "123456")
                  (department/created department-id school-id "Test Dept")
                  (student/class-assigned student-id department-id "TestKlas1")
                  (teacher/created teacher-id department-id "Leon E. Raar")
                  (teacher/class-assigned teacher-id department-id "TestKlas1")]]
      (doseq [chunk (partition-by message/primary-aggregate-id events)]
        (info [:appending chunk])
        (event-store/append-events (:store event-store) (message/primary-aggregate-id (first chunk)) -2 chunk)))
    component)
  (stop [component]
    (info "Stopping event fixtures, not removing anything")
    component))

(defn dev-event-fixtures-component []
  (map->DevEventFixturesComponent {}))
