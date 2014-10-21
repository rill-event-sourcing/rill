(ns studyflow.login.edu-route-mock-service
  (:require [studyflow.login.edu-route-service :refer [EduRouteService]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(deftype EduRouteMockService []
  EduRouteService

  (check-edu-route-signature [service edu-route-session-id signature]
    true)

  (get-student-info [service edu-route-session-id]
    {:edu-route-id "1234"
     :full-name "Joost M. Student"
     :brin-code "ABC1234"
     :raw-edu-route-response nil})

  (get-school-info [service brin-code]
    {
     "ASSUnr" "44544"
     "Naam" "RSG Magister Alvinus"
     "Straat" "Almastraat"
     "Huisnummer" "5"
     "Postcode" "8601 EW"
     "Woonplaats" "SNEEK"
     "Brin" "16FP00"}))

(defn edu-route-mock-service
  []
  (->EduRouteMockService))
