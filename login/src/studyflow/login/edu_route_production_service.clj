(ns studyflow.login.edu-route-production-service
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [studyflow.login.edu-route-service :refer [EduRouteService]]
            [digest :refer [md5]])
  (:import [eduroute EdurouteDistributeurService KeyPortType]))

(def pre-shared-key "DDF9nh3w45s$Wo1w")
(def leveranciers-code "studyflow")
(def controle-code "qW3#f65S")

(defn- make-full-name
  [voornaam tussenvoegsel achternaam]
  (if (and tussenvoegsel
           (= tussenvoegsel ""))
    (str voornaam " " tussenvoegsel " " achternaam)
    (str voornaam " " achternaam)))

(defn- make-edu-route-student-info
  [response]
  {
   :edu-route-id (response "ID")
   :brin-code (response "brin")
   :full-name (make-full-name (response "voornaam") (response "tussenvoegsel") (response "achternaam"))
   :raw-edu-route-response response})


(deftype EduRouteProductionService []
  EduRouteService

  (check-edu-route-signature [service edu-route-session-id signature]
    (= (digest/md5 (str edu-route-session-id pre-shared-key)) signature))

  ;; the WDSL is downloaded from the "http://www.eduroute.nl/soap/uitgever/uitgeverAPI.php?wsdl"
  ;; the encoded type is renamed to literal
  ;; a Java interface class is extracted using 'wsimport -verbose -Xnocompile -p eduroute wsdl-file'
  ;; This class can now be used to talk to the EduRouteService

  ;; Checkt de login van een account via een sessie
  ;; input: leverancierCode controleCode sessieID
  ;; output: ResParam ResString (via Holders)
  (get-student-info [service edu-route-session-id]
    (let [soapie (EdurouteDistributeurService.)
          myport (.getMyPort soapie)
          h1 (javax.xml.ws.Holder.)
          h2 (javax.xml.ws.Holder.)
          result (.sessieLogin myport leveranciers-code controle-code edu-route-session-id h1 h2)]
      (when (> (.value h1) 0)
        (make-edu-route-student-info (json/read-str (.value h2))))))

  ;; Vraagt de gegevens van een ASSUnr of Brin op.
  ;; input: uitgeverCode controleCode assuNr brin
  ;; output: ResParam ResString (via Holders)
  (get-school-info [service brin-code]
    (let [soapie (EdurouteDistributeurService.)
          myport (.getMyPort soapie)
          h1 (javax.xml.ws.Holder.)
          h2 (javax.xml.ws.Holder.)
          assu-nr ""
          result (.getSchoolInfo myport leveranciers-code controle-code assu-nr brin-code h1 h2)]
      (when (> (.value h1) 0)
        (json/read-str (.value h2))))))


(defn edu-route-production-service
  []
  (->EduRouteProductionService))
