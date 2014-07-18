(ns studyflow.login.edu-route-production-service
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [studyflow.login.edu-route-service :refer [EduRouteService]])
  (:import [eduroutenamespace EdurouteDistributeurService KeyPortType]))

(deftype EduRouteProductionService []
  EduRouteService
  (get-student-info [service edu-route-session-id signature]
    ;; from the URL "http://www.eduroute.nl/soap/uitgever/uitgeverAPI.php?wsdl"
    ;; a Java interface class is extracted using 'wsimport -verbose -Xcompile URL'
    ;; This class can now be used to talk to the EduRouteService
    (let [soapie (eduroutenamespace.EdurouteDistributeurService.)
          myport (.getMyPort soapie)
          h1 (javax.xml.ws.Holder.)
          h2 (javax.xml.ws.Holder.)
          result (.sessieLogin myport "studyflow" "qW3#f65S" "bcefd3636bf805056a81ae1cb9429197" h1 h2)]
      (when (> (.value h1) 0)
        (json/read-str (.value h2))))))

(defn edu-route-production-service
  []
  (->EduRouteProductionService))
