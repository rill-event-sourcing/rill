(ns studyflow.teaching.web.query
  (:require [compojure.core :refer [routes]]
            [studyflow.teaching.web.pages.completion :refer [completion-routes]]
            [studyflow.teaching.web.pages.manuals :refer [manuals-routes]]
            [studyflow.teaching.web.pages.chapter-list :refer [chapter-list-routes]]))

(def app
  (routes completion-routes
          manuals-routes
          chapter-list-routes))
