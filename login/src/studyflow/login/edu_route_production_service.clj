(ns studyflow.login.edu-route-production-service
  (:require [clojure.tools.logging :as log]
            [clj-soap.core :as soap]
            [studyflow.login.edu-route-service :refer [EduRouteService]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(deftype EduRouteProductionService []
  EduRouteService
  (get-student-info [service edu-route-session-id signature]
    (let [client (soap/client-fn "http://www.eduroute.nl/soap/uitgever/uitgeverAPI.php?wsdl")]
      (client :sessieLogin {:LeverancierCode "studyflow" :ControleCode "qW3#f65S" :SessieID edu-route-session-id})))
  Lifecycle
  (start [component]
    component)
  (stop [component]
    component))

