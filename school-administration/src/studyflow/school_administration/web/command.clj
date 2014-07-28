(ns studyflow.school-administration.web.command
  (:require [studyflow.school-administration.web.departments.command :as departments]
            [studyflow.school-administration.web.schools.command :as schools]
            [studyflow.school-administration.web.students.command :as students]))

(defn wrap-event-store [app store]
  #(app (assoc % :event-store store)))

(defn commands-app [event-store]
  (-> (fn [req] (some #(% req) [students/commands schools/commands departments/commands]))
      (wrap-event-store event-store)))
