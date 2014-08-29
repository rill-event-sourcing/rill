(ns studyflow.school-administration.web.query
  (:require [hiccup.page :refer [html5]]
            [rill.uuid :refer [uuid]]
            [studyflow.school-administration.read-model :as read-model]
            [studyflow.school-administration.web.departments.query :as departments]
            [studyflow.school-administration.web.schools.query :as schools]
            [studyflow.school-administration.web.students.query :as students]
            [studyflow.school-administration.web.teachers.query :as teachers]))

(defn model-up-to-date?
  [model id version]
  (<= version (or (read-model/aggregate-version model id) -1)))

(defn wrap-read-model
  [app model-atom]
  (fn [{{version :aggregate-version id :aggregate-id count :refresh-count :as flash} :flash :as request}]
    (let [read-model @model-atom
          count (or count 0)]
      (if (and version id count (< count 5)
               (not (model-up-to-date? read-model (uuid id) version)))
        {:status 200
         :headers {"Refresh" "0.2", "Content-Type" "text/html"}
         :body (html5 [:body "Just a sec..."])
         :flash (assoc flash :refresh-count (inc count))}
        (app (assoc request :read-model read-model))))))

(defn queries-app [read-model]
  (-> (fn [req] (some #(% req) [students/queries schools/queries departments/queries teachers/queries]))
      (wrap-read-model read-model)))
