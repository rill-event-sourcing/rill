(ns studyflow.learning.course.fixture
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [studyflow.json-tools :refer [key-from-json]]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course :as course]
            [studyflow.learning.course.events :as events]
            [studyflow.learning.course.commands :as commands]
            [rill.aggregate :refer [load-aggregate]]))

(def course-json
  (json/parse-string (slurp (io/resource "dev/material.json")) key-from-json))

(def course-edn
  (material/parse-course-material course-json))

(def course-id (:id course-edn))
(def course-published-event (events/published (:id course-edn) course-edn))
(def publish-course! (commands/publish! (:id course-edn) course-edn))
(def course-aggregate (load-aggregate [course-published-event]))



