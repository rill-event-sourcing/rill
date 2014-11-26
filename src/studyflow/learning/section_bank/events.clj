(ns studyflow.learning.section-bank.events
  (:use [rill.message :refer [defevent]]
        [studyflow.learning.course-material :as m]
        [schema.core :as s]))

(defn section-bank-id
  [{:keys [student-id section-id]}]
  (str "section-bank:" student-id ":" section-id))

(defevent CoinsEarned
  :section-id m/Id
  :student-id m/Id
  :amount s/Int
  section-bank-id)
