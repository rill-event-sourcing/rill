(ns studyflow.learning.t-read-model
  (:require [midje.sweet :refer :all]
            [studyflow.learning.read-model :as model]
            [studyflow.events :as events]
            [rill.uuid :refer [new-id]]))

(def editor-id 333)

(def initial-model
  (model/update-model {}
                      [
                       (events/map->LearningStepPublished
                        {:msg-id (new-id)
                         :learning-step-id 2
                         :publisher-id editor-id
                         :title "Position and 0"})
                       (events/map->ChapterPublished
                        {:msg-id (new-id)
                         :chapter-id 1
                         :publisher-id editor-id
                         :title "Positional notation"
                         :description "Positional notation or place-value notation is a method of representing or encoding numbers. Positional notation is distinguished from other notations (such as Roman numerals) for its use of the same symbol for the different orders of magnitude."
                         :learning-step-ids [2]})
                       (events/map->ChapterPublished
                        {:msg-id (new-id)
                         :chapter-id 3
                         :publisher-id editor-id
                         :title "Another chapter"
                         :description "Bla bla bla"
                         :learning-step-ids []})
                       (events/map->CourseUpdated
                        {:msg-id (new-id)
                         :course-id 6
                         :publisher-id editor-id
                         :title "Math"
                         :chapter-ids [1 3]})]))

(facts "Published chapters and learning steps can be requested by ID"
       (:title (model/get-chapter initial-model 1)) => "Positional notation"
       (:title (model/get-learning-step initial-model 2)) => "Position and 0")

(facts "Chapters and learning steps can be requested as a tree"
       (model/course-tree initial-model 6) =>
       {:id 6
        :title "Math"
        :chapters [{:id 1
                    :title "Positional notation"
                    :learning-steps [{:id 2
                                      :title "Position and 0"}]}
                   {:id 3
                    :title "Another chapter"
                    :learning-steps []}]})

