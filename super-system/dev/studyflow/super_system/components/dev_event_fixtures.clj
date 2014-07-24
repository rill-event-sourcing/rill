(ns studyflow.super-system.components.dev-event-fixtures
  (:require [crypto.password.bcrypt :as bcrypt]
            [com.stuartsierra.component :refer [Lifecycle]]
            [clojure.string :as string]
            [clojure.tools.logging :refer [info debug spy]]
            [rill.event-store :as event-store]
            [rill.message :as message]
            [rill.uuid :as uuid]
            [studyflow.events.student :as events-student]))

(defrecord DevEventFixturesComponent [event-store]
  Lifecycle
  (start [component]
    (info "Loading event fixtures")
    (let [student-id (uuid/new-id)
          stream-id student-id
          from-version -1
          events [{message/type :studyflow.school-administration.student.events/Created
                   :student-id student-id
                   :full-name "Dev Student One"}
                  (events-student/credentials-added student-id "dev-student-one@studyflow.nl" (bcrypt/encrypt "student"))]]
      (event-store/append-events (:store event-store) stream-id from-version events))
    component)
  (stop [component]
    (info "Stopping event fixtures, not removing anything")
    component))

(defn dev-event-fixtures-component []
  (map->DevEventFixturesComponent {}))
