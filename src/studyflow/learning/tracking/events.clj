(ns studyflow.learning.tracking.events
  (:require [rill.message :refer [defevent]]
            [schema.core :as s]
            [studyflow.learning.course-material :as m]))

(defn tracking-id [{:keys [student-id]}]
  (str "tracking:" student-id))

(defevent DashboardNavigated
  :student-id m/Id
  tracking-id)

(defevent EntryQuizNavigated
  :student-id m/Id
  tracking-id)

(defevent ChapterQuizNavigated
  :student-id m/Id
  :chapter-id m/Id
  tracking-id)

(defevent SectionExplanationNavigated
  :student-id m/Id
  :section-id m/Id
  tracking-id)

(defevent SectionTestNavigated
  :student-id m/Id
  :section-id m/Id
  tracking-id)
