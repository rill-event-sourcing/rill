(ns studyflow.school-administration.student.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]))

(def EmailPasswordCredentials
  {:email s/Str
   :encryped-password s/Str})

(defevent Created
  :student-id s/Uuid
  :full-name s/Str)

(defevent CredentialsAdded
  :student-id s/Uuid
  :credentials EmailPasswordCredentials)

(defevent CredentialsChanged
  :student-id s/Uuid
  :credentials EmailPasswordCredentials)

(defevent NameChanged
  :student-id s/Uuid
  :full-name s/Str)

(def fixture
  [(created "1" "Joost")
   (created "2" "Steven")
   (created "3" "Davide")
   (credentials-added "1" {:email "joost@zeekat.nl" :encryped-password "...."})])


