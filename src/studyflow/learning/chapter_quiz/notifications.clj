(ns studyflow.learning.chapter-quiz.notifications
  (:require [rill.message :as message]))

(defmulti notify
  (fn [chapter-quiz event & aggregates]
    (message/type event)))
