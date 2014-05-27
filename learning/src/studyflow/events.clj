(ns studyflow.events
  (:require [rill.message :refer [defevent]]))

(defevent CoursePublished
  [course-id publisher-id title chapter-ids])

(defevent CourseUpdated
  [course-id publisher-id title chapter-ids])

(defevent CourseArchived
  [course-id])

(defevent ChapterPublished
  [chapter-id publisher-id title description learning-step-ids])

(defevent ChapterArchived
  [chapter-id publisher-id])

(defevent ChapterUpdated
  [chapter-id publisher-id title description learning-step-ids])

(defevent LearningStepPublished
  [learning-step-id publisher-id title task-ids])

(defevent LearningStepArchived
  [learning-step-id publisher-id])

(defevent LearningStepUpdated
  [learning-step-id publisher-id title task-ids])

(defevent TaskPublished
  [task-id publisher-id title])

(defevent TaskArchived
  [task-id publisher-id])

(defevent TaskUpdated
  [task-id publisher-id title])



