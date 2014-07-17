(ns studyflow.login.edu-route-mock-service
  (:require [studyflow.login.edu-route-service :refer [EduRouteService]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(deftype EduRouteMockService []
  EduRouteService
  (get-student-info [service edu-route-session-id signature]
    {:edu-route-id "1234"
     :full-name "Joost M. Student"
     :brin-code "ABC1234"})
  Lifecycle
  (start [component]
    component)
  (stop [component]
    component))

(defn edu-route-mock-service
  []
  (->EduRouteMockService))
