(ns studyflow.teaching.web.query
  (:require [compojure.core :refer [routes]]
            [studyflow.teaching.web.reports.completion :refer [completion-routes]]
            [studyflow.teaching.web.reports.chapter-list :refer [chapter-list-routes]]))

(def app
  (routes completion-routes
          chapter-list-routes))
