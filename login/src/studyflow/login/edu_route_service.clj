(ns studyflow.login.edu-route-service
  (:require [clojure.tools.logging :as log]
            [clj-soap.core :as soap]))

(defn get-student-info
  [token]
  {:edu-route-id "1234"
   :full-name "Joost M. Student"
   :brin-code "ABC1234"})

;; unfinished!
;;
;; (defn get-student-info2cg
;;   [token]
;;   (log/info "ok")
;;   (let [client (soap/client-fn "http://www.eduroute.nl/soap/uitgever/uitgeverAPI.php?wsdl")]
;;     (client :sessieLogin {:LeverancierCode "studyflow" :ControleCode "qW3#f65S" :SessieID token})))


(defn get-edo-route-id
  [token]
  (:edo-route-id (get-student-info token)))
