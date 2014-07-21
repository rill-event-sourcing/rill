(ns studyflow.login.edu-route-mock-service
  (:require [studyflow.login.edu-route-service :refer [EduRouteService]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(deftype EduRouteMockService []
  EduRouteService

  (get-student-info [service edu-route-session-id signature]
    {:edu-route-id "1234"
     :full-name "Joost M. Student"
     :brin-code "ABC1234"})

  (get-school-info [service assu-nr brin-code]
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
